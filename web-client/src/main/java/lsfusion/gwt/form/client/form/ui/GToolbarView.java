package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;

public class GToolbarView extends FlexPanel {
    public void addTool(Widget tool) {
        add(tool);
    }
}
