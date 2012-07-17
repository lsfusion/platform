package platform.gwt.view2.logics;

import platform.gwt.view2.GGroupObject;
import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.changes.GGroupObjectValue;

import java.io.Serializable;

public interface FormLogicsProvider {
    boolean isEditingEnabled();

    void changeGroupObject(GGroupObject group, GGroupObjectValue key);
    void executeEditAction(GPropertyDraw property, String actionSID);
    void executeEditAction(GPropertyDraw property, GGroupObjectValue key, String actionSID);
    void changeProperty(GPropertyDraw property, Serializable value);
}
