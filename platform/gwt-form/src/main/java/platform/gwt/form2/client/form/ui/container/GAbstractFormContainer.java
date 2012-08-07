package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.user.client.ui.*;
import platform.gwt.view2.GComponent;
import platform.gwt.view2.GContainer;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class GAbstractFormContainer {
    protected GContainer key;
    public Map<GComponent, Widget> childrenViews = new LinkedHashMap<GComponent, Widget>();

    public void add(GComponent childKey, Widget childView, int position) {
        childrenViews.put(childKey, childView);
        addToContainer(childKey, childView, position);
    }

    public void add(GComponent childKey, Widget childView) {
        add(childKey, childView, -1);
    }

    public void remove(GComponent childKey) {
        if (childrenViews.containsKey(childKey)) {
            removeFromContainer(childKey, childrenViews.remove(childKey));
        }
    }

    public GContainer getKey() {
        return key;
    }

    public boolean isTabbed() {
        return key.type.isTabbed();
    }

    public boolean isSplit() {
        return key.type.isSplit();
    }

    public boolean isInTabbedPane() {
        return key.container != null && key.container.type.isTabbed();
    }

    public boolean isChildVisible(GComponent child) {
        Widget childView = childrenViews.get(child);
        return childView != null && childView.isVisible() && containerHasChild(childView);
    }

    private Widget containerView;
    public Widget getContainerView() {
        if (containerView == null) {
            containerView = getUndecoratedView();
            containerView.setSize("100%", "100%");
            if (key.title != null && !key.container.type.isTabbed()) {
                CaptionPanel captionedPanel = new CaptionPanel(key.title);
                captionedPanel.setContentWidget(containerView);

                final ResizeLayoutPanel captionPanelWrapper = new ResizeLayoutPanel();
                captionPanelWrapper.setWidget(captionedPanel);
                captionPanelWrapper.setSize("100%", "100%");
                containerView = captionPanelWrapper;

//                captionedPanel.setSize("100%", "100%");
//                containerView = captionedPanel;
            }
        }
        return containerView;
    }

    protected abstract Widget getUndecoratedView();
    protected abstract boolean containerHasChild(Widget widget);
    protected abstract void addToContainer(GComponent childKey, Widget childView, int position);
    protected abstract void removeFromContainer(GComponent childKey, Widget childView);
    public void setTableCellSize(Widget child, String size, boolean width) {}
}
