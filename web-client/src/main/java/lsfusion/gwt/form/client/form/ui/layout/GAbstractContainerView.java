package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.GCaptionPanel;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.base.shared.GwtSharedUtils.relativePosition;

public abstract class GAbstractContainerView {
    protected final GContainer container;
    protected Widget view;

    protected final List<GComponent> children = new ArrayList<GComponent>();
    protected final List<Widget> childrenViews = new ArrayList<Widget>();

    protected GAbstractContainerView(GContainer container) {
        this.container = container;
    }

    public void add(GComponent child, Widget view) {
        assert child != null && view != null && container.children.contains(child);

        int index = relativePosition(child, container.children, children);

        children.add(index, child);
        childrenViews.add(index, view);

        addImpl(index, child, view);
    }

    public void remove(GComponent child) {
        int index = children.indexOf(child);
        if (index == -1) {
            throw new IllegalStateException("Child wasn't added");
        }

        children.remove(index);
        Widget view = childrenViews.remove(index);

        removeImpl(index, child, view);
    }

    public boolean hasChild(GComponent child) {
        return children.contains(child);
    }

    public int getChildrenCount() {
        return children.size();
    }

    public GComponent getChild(int index) {
        return children.get(index);
    }

    public Widget getChildView(int index) {
        return childrenViews.get(index);
    }

    protected boolean isTopContainerView() {
        return container.container == null;
    }

    protected boolean needCaption() {
        return (!isTopContainerView() && !container.container.isTabbed()) && container.caption != null;
    }

    protected FlexPanel wrapWithCaptionAndSetMargins(FlexPanel view) {
        view = needCaption() ? new GCaptionPanel(container.caption, view) : view;
        view.setMargins(container.marginTop, container.marginBottom, container.marginLeft, container.marginRight);
        return view;
    }

    public void onResize() {
        Widget view = getView();
        if (view instanceof RequiresResize) {
            ((RequiresResize) view).onResize();
        }
    }

    void updateLayout() {
        //do nothing by default
    }

    protected abstract void addImpl(int index, GComponent child, Widget view);
    protected abstract void removeImpl(int index, GComponent child, Widget view);
    public abstract Widget getView();
}
