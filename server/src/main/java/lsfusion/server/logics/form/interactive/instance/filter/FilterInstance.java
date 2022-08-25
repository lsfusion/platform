package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.changed.Updated;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FilterInstance implements Updated {

    // даже если не в интерфейсе все равно ставить (то есть по сути фильтр делать false)
    public final static boolean ignoreInInterface = true;
    public boolean junction; //true - conjunction, false - disjunction

    public FilterInstance() {
    }

    protected abstract void fillObjects(MSet<ObjectInstance> objects);

    public ImSet<ObjectInstance> getObjects() {
        MSet<ObjectInstance> objects = SetFact.mSet();
        fillObjects(objects);
        return objects.immutable();
    }

    public FilterInstance(DataInputStream inStream, FormInstance form) {
    }

    public static FilterInstance deserialize(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
        CompareFilterInstance filter = new CompareFilterInstance(inStream, form);
        if (filter.value instanceof NullValue) {
            FilterInstance notNullFilter = new NotNullFilterInstance(filter.property);
            notNullFilter.junction = filter.junction;
            if (!filter.negate) {
                NotFilterInstance notFilter = new NotFilterInstance(notNullFilter);
                notFilter.junction = notNullFilter.junction;
                return notFilter;
            } else {
                return notNullFilter;
            }
        } else if (filter.value instanceof DataObject) {
            ConcreteClass filterValueClass = ((DataObject) filter.value).objectClass;

            if (filterValueClass instanceof StringClass) {

                boolean isContains = filter.compare == Compare.CONTAINS;
                boolean isEquals = filter.compare == Compare.EQUALS;

                String filterValue = (String) ((DataObject) filter.value).object;

                if ((isContains || isEquals) && (filterValue.contains(","))) {
                    FilterInstance resultFilter = null;
                    //one or more repetitions of \ and then any one char, or any char but \ and ,
                    Matcher matcher = Pattern.compile("(?:\\\\.|[^\\\\,])+", Pattern.DOTALL).matcher(filterValue);
                    while (matcher.find()) {
                        String value = matcher.group();
                        if (needWrapContains(value, isContains)) {
                            value = wrapContains(value);
                        }
                        CompareFilterInstance filterInstance = new CompareFilterInstance(filter.property, filter.resolveAdd, filter.toDraw, filter.negate, filter.compare, new DataObject(value, filterValueClass));

                        resultFilter = resultFilter == null ? filterInstance : new OrFilterInstance(resultFilter, filterInstance);

                    }
                    return resultFilter;
                } else if (needWrapContains(filterValue, isContains)) {
                    filter.value = new DataObject(wrapContains(filterValue), filterValueClass);
                }
            }
        }
        return filter;
    }

    private static boolean needWrapContains(String value, boolean isContains) {
        return isContains && !value.startsWith("%") && !value.endsWith("%");
    }

    private static String wrapContains(String value) {
        return "%" + value + "%";
    }

    public abstract GroupObjectInstance getApplyObject();

    public abstract Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, MSet<Property> mUsedProps) throws SQLException, SQLHandledException;

    public void resolveAdd(ExecutionEnvironment env, CustomObjectInstance object, DataObject addObject, ExecutionStack stack) throws SQLException, SQLHandledException {
    }

    public abstract NotNullFilterInstance notNullCached();

    // mappers in both directions
    protected static <X extends PropertyInterface> PropertyImplement<X, PropertyObjectInterfaceInstance> getPropertyImplement(PropertyObjectInstance<X> propertyObject) {
        return new PropertyImplement<>(propertyObject.property, propertyObject.mapping);
    }
    protected static <X extends PropertyInterface> PropertyImplement<X, PropertyObjectInterfaceInstance> getPropertyImplement(NotNullFilterInstance<X> notNullFilter) {
        return getPropertyImplement(notNullFilter.property);
    }
    protected static <X extends PropertyInterface> PropertyObjectInstance<X> getPropertyObjectInstance(PropertyImplement<X, PropertyObjectInterfaceInstance> propertyImplement) {
        return new PropertyObjectInstance<>(propertyImplement.property, propertyImplement.mapping);
    }
    public static <X extends PropertyInterface> PropertyObjectInstance<X> getPropertyObjectInstance(PropertyRevImplement<X, ObjectInstance> propertyImplement) {
        return new PropertyObjectInstance<>(propertyImplement.property, propertyImplement.mapping);
    }
    protected static <X extends PropertyInterface> NotNullFilterInstance<X> getFilterInstance(PropertyImplement<X, PropertyObjectInterfaceInstance> propertyImplement) {
        return new NotNullFilterInstance<>(getPropertyObjectInstance(propertyImplement));
    }

    public static <X extends PropertyInterface> NotNullFilterInstance combineCached(ImSet<FilterInstance> filters, boolean and) {
        if(filters.size() == 1)
            return filters.single().notNullCached();

        ImSet<PropertyImplement<?, PropertyObjectInterfaceInstance>> operands = filters.mapSetValues(filterInstance -> getPropertyImplement(filterInstance.notNullCached()));

        PropertyImplement<?, PropertyObjectInterfaceInstance> resultProperty = and ? PropertyFact.createAndCached(operands) : PropertyFact.createOrCached(operands);
        return getFilterInstance(resultProperty);
    }

    public static <P extends PropertyInterface, X extends PropertyInterface> PropertyObjectInstance ifCached(PropertyObjectInstance<P> propertyObject, ImSet<FilterInstance> filters) {
        NotNullFilterInstance<X> notNullFilterInstance = combineCached(filters, true);

        return getPropertyObjectInstance(PropertyFact.createIfCached(getPropertyImplement(propertyObject), getPropertyImplement(notNullFilterInstance)));
    }
}
