package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.FlexCaptionPanel;
import lsfusion.gwt.client.form.object.table.view.GridPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static lsfusion.gwt.client.base.GwtClientUtils.calculateMaxPreferredSize;
import static lsfusion.gwt.client.base.GwtClientUtils.enlargeDimension;
import static lsfusion.gwt.client.base.GwtSharedUtils.relativePosition;

public abstract class GAbstractContainerView {
    protected final GContainer container;

    protected final List<GComponent> children = new ArrayList<>();
    protected final List<Widget> childrenViews = new ArrayList<>();

    protected GAbstractContainerView(GContainer container) {
        this.container = container;
    }

    public void add(GComponent child, final Widget view) {
        assert child != null && view != null && container.children.contains(child);

        int index = relativePosition(child, container.children, children);

        children.add(index, child);
        childrenViews.add(index, view);

        addImpl(index, child, view);

        if(child.autoSize && view instanceof GridPanel)
            addUpdateLayoutListener(requestIndex -> ((GridPanel)view).autoSize());
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

    protected Dimension getChildMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews, int index) {
        return getChildMaxPreferredSize(containerViews, getChild(index));
    }

    protected Dimension getChildMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews, GComponent child) {
        Dimension dimensions = child instanceof GContainer
                               ? getMaxPreferredSize((GContainer) child, containerViews, true)
                               : getMaxPreferredSize(child, getChildView(child));
        Dimension result = enlargeDimension(dimensions, child.getHorizontalMargin(), child.getVerticalMargin());
        GFormLayout.setDebugDimensionsAttributes(getChildView(child), result);
        return result;
    }
    
    public static Dimension getMaxPreferredSize(GContainer child, Map<GContainer, GAbstractContainerView> containerViews, boolean max) {
        return overrideSize(child, containerViews.get(child).getMaxPreferredSize(containerViews), max);
    }
    private static Dimension getMaxPreferredSize(GComponent child, Widget childView) {
        return overrideSize(child, calculateMaxPreferredSize(childView), true);        
    }

    private static Dimension overrideSize(GComponent child, Dimension dimension, boolean max) {
        if(child.height == -1 && child.width == -1) // оптимизация
            return dimension;

        int preferredWidth = child.width;
        if(preferredWidth == -1)
            preferredWidth = dimension.width;
        else if(max)
            preferredWidth = Math.max(preferredWidth, dimension.width);

        int preferredHeight = child.height;
        if(preferredHeight == -1)
            preferredHeight = dimension.height;
        else if(max)
            preferredHeight = Math.max(preferredHeight, dimension.height);
        return new Dimension(preferredWidth, preferredHeight);
    }

    // не предполагает явное использование (так как не содержит проверки на явный size)
    protected Dimension getMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        boolean vertical = container.isVertical();
        int width = 0;
        int height = 0;
        int chCnt = children.size();
        for (int i = 0; i < chCnt; ++i) {
            if (childrenViews.get(i).isVisible()) {
                Dimension childSize = getChildMaxPreferredSize(containerViews, i);
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
        if (hasCaption()) {
            dimension.width += 10;
            dimension.height += 20;
        }
        return dimension;
    }

    private FlexCaptionPanel titledBorder;

    private boolean hasCaption() { // не top, не tabbed и есть caption
        return !container.isTab() && !container.main && container.caption != null;
    }

    protected FlexPanel initBorder(FlexPanel view) {
        if(hasCaption()) {
            titledBorder = new FlexCaptionPanel(container.caption, view);
            // updateCaption();
            return titledBorder;
        }
        return view;
    }

    public void updateCaption() {
        assert hasCaption();
        titledBorder.setCaption(container.caption);
    }

    public void onResize() {
        Widget view = getView();
        if (view instanceof RequiresResize) {
            ((RequiresResize) view).onResize();
        }
    }

    public interface UpdateLayoutListener {
        void updateLayout(long requestIndex);
    }

    private List<UpdateLayoutListener> updateLayoutListeners = new ArrayList<>();
    public void addUpdateLayoutListener(UpdateLayoutListener listener) {
        updateLayoutListeners.add(listener);
    }
    public void updateLayout(long requestIndex) {
        for(UpdateLayoutListener updateLayoutListener : updateLayoutListeners)
            updateLayoutListener.updateLayout(requestIndex);
    }

    private static Integer getSize(boolean vertical, boolean mainAxis, GComponent component) {
        int size;
        if (mainAxis) {
            size = vertical ? component.height : component.width;    
        } else {
            size = vertical ? component.width : component.height;
        }
        if (size != -1)
            return size;
        return null;
    }
    public static void add(FlexPanel panel, Widget widget, GComponent component, int beforeIndex) {
        boolean vertical = panel.isVertical();
        panel.add(widget, beforeIndex, component.getAlignment(), component.getFlex(), getSize(vertical, true, component), getSize(vertical, false, component));
    }

    protected abstract void addImpl(int index, GComponent child, Widget view);
    protected abstract void removeImpl(int index, GComponent child, Widget view);
    public abstract Widget getView();
}
