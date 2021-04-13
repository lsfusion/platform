package lsfusion.server.logics.navigator.controller.env;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

public abstract class ChangesController {

    protected abstract DBManager getDbManager();

    public void regChange(ImSet<Property> changes, DataSession session) {
        DBManager dbManager = getDbManager();
        if(dbManager != null)
            dbManager.registerChange(changes); // global changes (for async caches)
        regLocalChange(changes, session); // local changes (for form synchronization in one connection)
    }

    protected void regLocalChange(ImSet<Property> changes, DataSession session) {}

    public ImSet<Property> update(DataSession session, FormInstance form) { return SetFact.EMPTY(); }

    public void registerForm(FormInstance form) {}

    public void unregisterForm(FormInstance form) {}
}
