package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.shared.view.GComponent;
import platform.gwt.form2.shared.view.GContainer;

public class GFormFlowPanel extends GAbstractFormContainer {
    private FlowPanel panel;

    public GFormFlowPanel(GContainer key) {
        this.key = key;

        panel = new FlowPanel();
    }

    @Override
    protected Widget getUndecoratedView() {
        return panel;
    }

    @Override
    protected boolean containerHasChild(Widget widget) {
        return panel.getWidgetIndex(widget) != -1;
    }

    @Override
    protected void addToContainer(GComponent childKey, Widget childView, int position) {
        if (position == -1 || position >= childrenViews.size() - 1) {
            panel.add(childView);
        } else {
            panel.insert(childView, position);
        }
        if (childKey.hAlign.equals(GContainer.Alignment.RIGHT)) {
            childView.addStyleName("flowPanelChildRightAlign");
        } else {
            childView.addStyleName("flowPanelChildLeftAlign");
        }
    }

    @Override
    protected void removeFromContainer(GComponent childKey, Widget childView) {
        panel.remove(childView);
    }

    @Override
    public void setChildSize(GComponent child, String width, String height) {
        Widget childView = childrenViews.get(child);
        if (childView != null) {
            if (width != null) {
                childView.setWidth(width);
            }
            if (height != null) {
                childView.setHeight(height);
            }
        }
    }
}
