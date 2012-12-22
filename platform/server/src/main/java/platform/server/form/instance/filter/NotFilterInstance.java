package platform.server.form.instance.filter;

import platform.base.FunctionSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.property.CalcProperty;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class NotFilterInstance extends FilterInstance {

    FilterInstance filter;

    public NotFilterInstance(FilterInstance filter) {
        this.filter = filter;
    }

    protected NotFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        super(inStream, form);
        filter = deserialize(inStream, form);
    }

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return filter.classUpdated(gridGroups);
    }

    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return filter.objectUpdated(gridGroups);
    }

    public boolean dataUpdated(FunctionSet<CalcProperty> changedProps) {
        return filter.dataUpdated(changedProps);
    }

    public void fillProperties(MSet<CalcProperty> properties) {
        filter.fillProperties(properties);
    }

    public GroupObjectInstance getApplyObject() {
        return filter.getApplyObject();
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        return filter.getWhere(mapKeys, modifier).not();
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return filter.isInInterface(classGroup);
    }
}
