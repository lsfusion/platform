package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.changed.ChangedData;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class NotFilterInstance extends FilterInstance {

    private final FilterInstance filter;

    public NotFilterInstance(FilterInstance filter) {
        this.filter = filter;
    }

    protected NotFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
        super(inStream, form);
        filter = deserialize(inStream, form);
    }

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return filter.classUpdated(gridGroups);
    }

    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return filter.objectUpdated(gridGroups);
    }

    public boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier, boolean hidden, ImSet<GroupObjectInstance> groupObjects) throws SQLException, SQLHandledException {
        return filter.dataUpdated(changedProps, reallyChanged, modifier, hidden, groupObjects);
    }

    public void fillProperties(MSet<Property> properties) {
        filter.fillProperties(properties);
    }

    public GroupObjectInstance getApplyObject() {
        return filter.getApplyObject();
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, MSet<Property> mUsedProps) throws SQLException, SQLHandledException {
        return filter.getWhere(mapKeys, modifier, reallyChanged, mUsedProps).not();
    }

    protected void fillObjects(MSet<ObjectInstance> objects) {
        filter.fillObjects(objects);
    }

    @Override
    public NotNullFilterInstance notNullCached() {
        return getFilterInstance(PropertyFact.createNotCached(getPropertyImplement(filter.notNullCached())));
    }
}
