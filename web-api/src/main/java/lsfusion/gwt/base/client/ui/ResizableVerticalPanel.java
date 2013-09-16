package lsfusion.gwt.base.client.ui;

import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;

import static lsfusion.gwt.base.client.GwtClientUtils.calculateStackPreferredSize;

public class ResizableVerticalPanel extends VerticalPanel implements RequiresResize, ProvidesResize, HasPreferredSize {
    @Override
    public void onResize() {
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
        return calculateStackPreferredSize(this.iterator(), true);
    }
}
