package lsfusion.gwt.base.client.ui;

import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.Dimension;

import static lsfusion.gwt.base.client.GwtClientUtils.calculateStackPreferredSize;

public class ResizableHorizontalPanel extends HorizontalPanel implements RequiresResize, ProvidesResize, HasPreferredSize {
    @Override
    public void onResize() {
        if (!visible) {
            return;
        }
        for (Widget child : this) {
            if (child instanceof RequiresResize) {
                ((RequiresResize) child).onResize();
            }
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
        return calculateStackPreferredSize(this.iterator(), false);
    }
}
