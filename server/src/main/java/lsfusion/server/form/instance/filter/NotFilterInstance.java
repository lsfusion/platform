package lsfusion.server.form.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.session.Modifier;

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

    public boolean dataUpdated(ChangedData changedProps, Modifier modifier) {
        return filter.dataUpdated(changedProps, modifier);
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
