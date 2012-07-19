package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
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
        return key.container != null && key.container.type.isSplit();
    }

    public boolean isInSplitPane() {
        return key.container != null && key.container.type.isTabbed();
    }

    public boolean isChildVisible(GComponent child) {
        Widget childView = childrenViews.get(child);
        return childView != null && childView.isVisible() && containerHasChild(childView);
    }

    private Panel containerView;
    public Panel getContainerView() {
        if (containerView == null) {
            containerView = getUndecoratedView();
            if (key.title != null && !key.container.type.isTabbed()) {
                CaptionPanel captionedPanel = new CaptionPanel(key.title);
                captionedPanel.add(containerView);
                containerView = new SimplePanel(captionedPanel);
            }
        }
        return containerView;
    }

    protected abstract Panel getUndecoratedView();
    protected abstract boolean containerHasChild(Widget widget);
    protected abstract void addToContainer(GComponent childKey, Widget childView, int position);
    protected abstract void removeFromContainer(GComponent childKey, Widget childView);
}
