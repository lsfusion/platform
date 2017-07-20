package lsfusion.gwt.base.client.ui;

import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.GwtClientUtils;

public class ResizableLayoutPanel extends ResizeLayoutPanel implements RequiresResize, ProvidesResize, HasPreferredSize {
    
    public ResizableLayoutPanel() {
        this(null);
    }
    
    public ResizableLayoutPanel(Widget widget) {
        if (widget != null) {
            setWidget(widget);
        }
    }

    @Override
    public void onResize() {
        if (!visible) {
            return;
        }
        Widget child = getWidget();
        if (child instanceof RequiresResize) {
            ((RequiresResize) child).onResize();
        }
    }

    boolean visible = true;
    @Override
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            super.setVisible(visible);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return GwtClientUtils.calculatePreferredSize(getWidget());
    }
}
