package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class GToolbarView extends HorizontalPanel {
    public void addTool(Widget tool) {
        add(tool);
    }
}
