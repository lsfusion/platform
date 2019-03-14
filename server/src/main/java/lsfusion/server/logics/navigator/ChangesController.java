package lsfusion.server.logics.navigator;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.action.session.DataSession;

public interface ChangesController {

    void regChange(ImSet<Property> changes, DataSession session);

    ImSet<Property> update(DataSession session, FormInstance form);

    void registerForm(FormInstance form);

    void unregisterForm(FormInstance form);
}
