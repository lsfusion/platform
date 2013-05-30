package platform.server.form.instance.filter;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcPropertyValueImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.ExecutionEnvironment;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

public abstract class OpFilterInstance extends FilterInstance {

    public FilterInstance op1;
    public FilterInstance op2;

    public OpFilterInstance(FilterInstance op1, FilterInstance op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    protected OpFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        super(inStream, form);
        op1 = deserialize(inStream, form);
        op2 = deserialize(inStream, form);
    }

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return op1.classUpdated(gridGroups) || op2.classUpdated(gridGroups);
    }

    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return op1.objectUpdated(gridGroups) || op2.objectUpdated(gridGroups);
    }

    public boolean dataUpdated(FunctionSet<CalcProperty> changedProps) {
        return op1.dataUpdated(changedProps) || op2.dataUpdated(changedProps);
    }

    public void fillProperties(MSet<CalcProperty> properties) {
        op1.fillProperties(properties);
        op2.fillProperties(properties);
    }

    public GroupObjectInstance getApplyObject() {
        GroupObjectInstance apply1 = op1.getApplyObject();
        GroupObjectInstance apply2 = op2.getApplyObject();
        if(apply2==null || (apply1!=null && apply1.order>apply2.order))
            return apply1;
        else
            return apply2;
    }

    @Override
    public void resolveAdd(ExecutionEnvironment env, CustomObjectInstance object, DataObject addObject) throws SQLException {
        op1.resolveAdd(env, object, addObject);
        op2.resolveAdd(env, object, addObject);
    }

    @Override
    public <X extends PropertyInterface> Set<CalcPropertyValueImplement<?>> getResolveChangeProperties(CalcProperty<X> toChange) {
        return BaseUtils.mergeSet(op1.getResolveChangeProperties(toChange), op2.getResolveChangeProperties(toChange));
    }
}
