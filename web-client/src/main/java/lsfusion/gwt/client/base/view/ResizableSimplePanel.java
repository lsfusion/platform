package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;

import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;

public class ResizableSimplePanel extends SimplePanel implements RequiresResize, ProvidesResize, ResizableMainPanel {
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

    // content-independent widget
    public void setFillWidget(Widget widget) {
        setWidget(widget);
        setupFillParent(widget.getElement());        
    }

    // content-dependent widget
    public void setPercentWidget(Widget widget) {
        setWidget(widget);
        GwtClientUtils.setupPercentParent(widget.getElement());
    }

    public void changePercentFillWidget(boolean percent) {
        GwtClientUtils.changePercentFillWidget(getWidget(), percent);
    }

    @Override
    public void setFillMain(Widget main) {
        setFillWidget(main);
    }

    @Override
    public void setPercentMain(Widget main) {
        setPercentWidget(main);
    }

    @Override
    public Widget getPanelWidget() {
        return this;
    }
}
