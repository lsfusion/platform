package lsfusion.gwt.base.client.ui;

import com.google.gwt.user.client.ui.*;

public class ResizableLayoutPanel extends ResizeLayoutPanel implements RequiresResize, ProvidesResize {
    @Override
    public void onResize() {
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
}
