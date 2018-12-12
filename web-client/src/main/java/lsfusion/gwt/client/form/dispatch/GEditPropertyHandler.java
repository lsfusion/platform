package lsfusion.gwt.client.form.dispatch;

import lsfusion.gwt.shared.view.classes.GType;

public interface GEditPropertyHandler {
    void requestValue(GType valueType, Object oldValue);

    void updateEditValue(Object value);

    void takeFocusAfterEdit();
}
