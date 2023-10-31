package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;

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

    public void setPercentMain(Widget widget) {
        setWidget(widget);
        Element element = widget.getElement();
        GwtClientUtils.setupPercentParent(element);
        GwtClientUtils.setupFillParent(element);
    }

    @Override
    public Widget getPanelWidget() {
        return this;
    }
}
