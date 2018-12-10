package lsfusion.gwt.client.form.form.ui;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.ui.ResizableHorizontalPanel;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;

public class GToolbarView extends ResizableHorizontalPanel {
    public void addTool(Widget tool) {
        add(tool);
    }

    @Override
    public Dimension getMaxPreferredSize() {
        return getOffsetSize(this);
    }
}
