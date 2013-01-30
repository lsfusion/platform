package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.ui.ResizableHorizontalPanel;

public class GToolbarView extends ResizableHorizontalPanel {
    public void addTool(Widget tool) {
        add(tool);
    }
}
