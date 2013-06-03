package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

public class PropertyLabel extends Label {
    private GPropertyDraw property;

    public PropertyLabel(GPropertyDraw property) {
        super(property.getNotEmptyCaption(), false);
        this.property = property;
    }

    public GPropertyDraw getProperty() {
        return property;
    }
}
