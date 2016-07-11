package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.layout.flex.FlexCaptionPanel;
import lsfusion.gwt.form.client.form.ui.layout.table.TableCaptionPanel;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static lsfusion.gwt.base.client.GwtClientUtils.calculatePreferredSize;
import static lsfusion.gwt.base.client.GwtClientUtils.enlargeDimension;
import static lsfusion.gwt.base.shared.GwtSharedUtils.relativePosition;

public abstract class GAbstractContainerView {
    protected final GContainer container;
    protected Widget view;

    protected final List<GComponent> children = new ArrayList<>();
    protected final List<Widget> childrenViews = new ArrayList<>();

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

    public Widget getChildView(GComponent child) {
        int index = children.indexOf(child);
        return index != -1 ? childrenViews.get(index) : null;
    }

    protected boolean isTopContainerView() {
        return container.container == null;
    }

    protected Dimension getChildPreferredSize(Map<GContainer, GAbstractContainerView> containerViews, int index) {
        return getChildPreferredSize(containerViews, getChild(index));
    }

    protected Dimension getChildPreferredSize(Map<GContainer, GAbstractContainerView> containerViews, GComponent child) {
        Dimension dimensions = child instanceof GContainer
                               ? containerViews.get(child).getPreferredSize(containerViews)
                               : calculatePreferredSize(getChildView(child));
        Dimension result = enlargeDimension(dimensions, child.getHorizontalMargin(), child.getVerticalMargin());
        GFormLayout.setDebugDimensionsAttributes(getChildView(child), result);
        return result;
    }

    protected Dimension getChildrenStackSize(Map<GContainer, GAbstractContainerView> containerViews, boolean vertical) {
        int width = 0;
        int height = 0;
        int chCnt = children.size();
        for (int i = 0; i < chCnt; ++i) {
            if (childrenViews.get(i).isVisible()) {
                Dimension childSize = getChildPreferredSize(containerViews, i);
                if (vertical) {
                    width = max(width, childSize.width);
                    height += childSize.height;
                } else {
                    width += childSize.width;
                    height = max(height, childSize.height);
                }
            }
        }

        return addCaptionDimensions(new Dimension(width, height));
    }

    protected Dimension addCaptionDimensions(Dimension dimension) {
        if (needCaption()) {
            dimension.width += 10;
            dimension.height += 20;
        }
        return dimension;
    }

    protected boolean needCaption() {
        return (!isTopContainerView() && !container.container.isTabbed()) && container.caption != null;
    }

    protected FlexPanel wrapWithFlexCaption(FlexPanel view) {
        return needCaption() ? new FlexCaptionPanel(container.caption, view) : view;
    }

    protected Widget wrapWithTableCaption(Widget content) {
        return needCaption() ? new TableCaptionPanel(container.caption, content) : content;
    }

    public void onResize() {
        Widget view = getView();
        if (view instanceof RequiresResize) {
            ((RequiresResize) view).onResize();
        }
    }

    public void updateLayout() {
        //do nothing by default
    }

    protected abstract void addImpl(int index, GComponent child, Widget view);
    protected abstract void removeImpl(int index, GComponent child, Widget view);
    public abstract Widget getView();
    public abstract Dimension getPreferredSize(Map<GContainer, GAbstractContainerView> containerViews);
}
