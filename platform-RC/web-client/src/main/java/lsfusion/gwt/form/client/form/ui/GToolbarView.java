package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;

import static lsfusion.gwt.base.client.GwtClientUtils.getOffsetSize;

public class GToolbarView extends ResizableHorizontalPanel {
    public void addTool(Widget tool) {
        add(tool);
    }

    @Override
    public Dimension getPreferredSize() {
        return getOffsetSize(this);
    }
}
