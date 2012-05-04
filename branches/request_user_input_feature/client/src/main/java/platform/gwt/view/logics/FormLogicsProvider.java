package platform.gwt.view.logics;

import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GGroupObjectValue;

public interface FormLogicsProvider {
    boolean isEditingEnabled();

    void changeGroupObject(GGroupObject group, GGroupObjectValue key);
    void changePropertyDraw(GPropertyDraw property, Object value);
    void changePropertyDraw(GGroupObject group, GGroupObjectValue key, GPropertyDraw property, Object value);
    void selectObject(GPropertyDraw property, SelectObjectCallback selectObjectCallback);
}
