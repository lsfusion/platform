package platform.gwt.form2.client.form.dispatch;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.classes.GType;

public interface GEditPropertyHandler {
    public GPropertyDraw getProperty(int column);

    public void requestValue(GType valueType, Object oldValue);

    public void updateEditValue(Object value);
}
