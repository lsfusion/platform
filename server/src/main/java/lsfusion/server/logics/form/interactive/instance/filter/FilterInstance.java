package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.NullValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.ExecutionEnvironment;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.change.ReallyChanged;
import lsfusion.server.logics.form.interactive.change.Updated;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.Property;
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

    public FilterInstance(DataInputStream inStream, FormInstance form) throws IOException {
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

    public <X extends PropertyInterface> Set<PropertyValueImplement<?>> getResolveChangeProperties(Property<X> toChange) {
        return new HashSet<>();
    }

}
