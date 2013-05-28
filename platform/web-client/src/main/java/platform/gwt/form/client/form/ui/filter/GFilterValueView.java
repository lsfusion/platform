package platform.gwt.form.client.form.ui.filter;

import com.google.gwt.user.client.ui.SimplePanel;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditEvent;

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

    public void propertyChanged(GPropertyDraw property) {}

    public void startEditing(EditEvent keyEvent) {
        focusOnValue();
    }
}
