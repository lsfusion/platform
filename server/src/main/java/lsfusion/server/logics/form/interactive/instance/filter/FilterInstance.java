package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
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
import lsfusion.server.logics.property.implement.PropertyValueImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

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
        }
        else
            return filter;
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
