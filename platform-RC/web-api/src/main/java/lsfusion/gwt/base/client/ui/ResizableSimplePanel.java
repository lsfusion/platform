package lsfusion.gwt.base.client.ui;

import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.GwtClientUtils;

public class ResizableSimplePanel extends SimplePanel implements RequiresResize, ProvidesResize, HasPreferredSize {
    public ResizableSimplePanel() {
    }

    public ResizableSimplePanel(Widget child) {
        super(child);
    }

    @Override
    public void onResize() {
        if (!visible) {
            return;
        }
        Widget child  = getWidget();
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
