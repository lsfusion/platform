package lsfusion.gwt.form.client.form.dispatch;

import lsfusion.gwt.form.shared.view.classes.GType;

public interface GEditPropertyHandler {
    void requestValue(GType valueType, Object oldValue);

    void updateEditValue(Object value);

    void takeFocusAfterEdit();
}
