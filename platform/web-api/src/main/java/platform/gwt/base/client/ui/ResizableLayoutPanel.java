package platform.gwt.base.client.ui;

import com.google.gwt.user.client.ui.*;

public class ResizableLayoutPanel extends ResizeLayoutPanel implements RequiresResize, ProvidesResize {
    @Override
    public void onResize() {
        Widget child = getWidget();
        if (child instanceof RequiresResize) {
            ((RequiresResize) child).onResize();
        }
    }
}
