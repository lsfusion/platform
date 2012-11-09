package platform.gwt.form.client.form.dispatch;

import platform.gwt.form.shared.view.classes.GType;

public interface GEditPropertyHandler {
    public void requestValue(GType valueType, Object oldValue);

    public void updateEditValue(Object value);

    void setFocus(boolean focus);
}
