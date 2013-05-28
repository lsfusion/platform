package platform.gwt.form.client.form.ui.container;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.client.form.ui.GCaptionPanel;
import platform.gwt.form.shared.view.GComponent;
import platform.gwt.form.shared.view.GContainer;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class GAbstractFormContainer {
    protected GContainer key;
    public Map<GComponent, Widget> childrenViews = new LinkedHashMap<GComponent, Widget>();

    public void add(GComponent childKey, Widget childView, int position) {
        if (position == -1 || position >= childrenViews.size()) {
            childrenViews.put(childKey, childView);
        } else {
            LinkedHashMap<GComponent, Widget> newChildrenMap = new LinkedHashMap<GComponent, Widget>();
            int i = 0;
            for (GComponent child : childrenViews.keySet()) {
                if (i == position) {
                    newChildrenMap.put(childKey, childView);
                }
                newChildrenMap.put(child, childrenViews.get(child));
                i++;
            }
            childrenViews = newChildrenMap;
        }
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

    public boolean isInSplitPane() {
        return key.container != null && key.container.type.isSplit();
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
            if (key.title != null && key.container != null && !key.container.type.isTabbed()) {
                containerView = new GCaptionPanel(key.title, containerView);
            }
        }
        return containerView;
    }

    public void onResize() {
        Widget view = getContainerView();
        if (view instanceof RequiresResize) {
            ((RequiresResize) view).onResize();
        }
    }

    protected abstract Widget getUndecoratedView();
    protected abstract boolean containerHasChild(Widget widget);
    protected abstract void addToContainer(GComponent childKey, Widget childView, int position);
    protected abstract void removeFromContainer(GComponent childKey, Widget childView);
    public void setTableCellSize(Widget child, String size, boolean width) {}
    public void setChildSize(GComponent child, String width, String height) {}
    public void addDirectly(Widget child) {}
}
