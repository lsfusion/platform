package lsfusion.server.logics.navigator.controller.env;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

public abstract class ChangesController {

    protected abstract DBManager getDbManager();

    public void regChange(ImSet<Property> changes, DataSession session) {
        DBManager dbManager = getDbManager();
        if(dbManager != null)
            dbManager.registerChange(changes); // global changes (for async caches)
        regLocalChange(changes, session); // local changes (for form synchronization in one connection)
    }

    public void flushStrong(ImSet<Pair<Property, ImMap<PropertyInterface, ? extends ObjectValue>>> changedPropKeys) {
        DBManager dbManager = getDbManager();
        if(dbManager != null)
            dbManager.flushStrong(changedPropKeys);
    }

    public <T extends PropertyInterface> ObjectValue readLazyValue(Property<T> property, ImMap<T, ? extends ObjectValue> keys) throws SQLException, SQLHandledException {
        return getDbManager().readLazyValue(property, keys);
    }

    protected void regLocalChange(ImSet<Property> changes, DataSession session) {}

    public ImSet<Property> update(DataSession session, ChangesObject form) { return SetFact.EMPTY(); }

    public void registerForm(FormInstance form) {}

    public void unregisterForm(FormInstance form) {}
}
