package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;

import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;

public class ResizableSimplePanel extends SimplePanel implements RequiresResize, ProvidesResize, HasMaxPreferredSize, ResizableMainPanel {
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
    
    public void setFillWidget(Widget widget) { // adding element 100% fill this panel
        setWidget(widget);
        setupFillParent(widget.getElement());        
    }

    @Override
    public Dimension getMaxPreferredSize() {
        return GwtClientUtils.calculateMaxPreferredSize(getWidget());
    }

    @Override
    public void setFillMain(Widget main) {
        setFillWidget(main);
    }

    @Override
    public void setPercentMain(Widget main) {
        setWidget(main);
        GwtClientUtils.setupPercentParent(main.getElement());
    }

    @Override
    public Widget getPanelWidget() {
        return this;
    }
}
