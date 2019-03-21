package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;

public class GFilterValueView extends SimplePanel {
    protected GFilterValueListener listener;

    public GFilterValueView(GFilterValueListener listener) {
        this.listener = listener;
    }

    public interface GFilterValueListener {
        void valueChanged();
    }

    public void focusOnValue() {
    }

    public void propertyChanged(GPropertyDraw property, GGroupObjectValue columnKey) {}

    public void startEditing(EditEvent keyEvent) {
        focusOnValue();
    }
}
