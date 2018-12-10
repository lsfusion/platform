package lsfusion.gwt.form.client.form.ui.filter;

import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.client.grid.EditEvent;

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
