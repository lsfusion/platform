package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;

public class GToolbarView extends ResizableHorizontalPanel {
    public void addTool(Widget tool) {
        add(tool);
    }
}
