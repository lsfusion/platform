package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class GToolbarPanel extends FlowPanel {

    public GToolbarPanel() {
        super();
//        setAutoHeight();
//        setBackgroundColor("#F5F5F5");
    }

    public boolean isEmpty() {
        return getWidgetCount() == 0;
    }

    public void addComponent(Widget child) {
        if (getWidgetIndex(child) != -1) {
            remove(child);
        }
        add(child);
    }

    public void removeComponent(Widget child) {
        remove(child);
    }
}
