package platform.gwt.form2.shared.view.logics;

import platform.gwt.form2.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form2.shared.view.GGroupObject;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

import java.io.Serializable;

public interface FormLogicsProvider {
    void changeGroupObject(GGroupObject group, GGroupObjectValue key);
    void executeEditAction(GPropertyDraw property, String actionSID);
    void executeEditAction(GPropertyDraw property, GGroupObjectValue key, String actionSID);
    void changeProperty(GEditPropertyHandler editHandler, GPropertyDraw property, Serializable value, Object oldValue);
}
