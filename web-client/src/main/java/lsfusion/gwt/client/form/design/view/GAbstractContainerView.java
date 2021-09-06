package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static lsfusion.gwt.client.base.GwtClientUtils.calculateMaxPreferredSize;
import static lsfusion.gwt.client.base.GwtClientUtils.enlargeDimension;
import static lsfusion.gwt.client.base.GwtSharedUtils.relativePosition;

public abstract class GAbstractContainerView {
    protected final GContainer container;

    protected final boolean vertical;

    protected final List<GComponent> children = new ArrayList<>();
    protected final List<Widget> childrenViews = new ArrayList<>();

    protected GAbstractContainerView(GContainer container) {
        this.container = container;

        vertical = container.isVertical();
    }

    public void add(GComponent child, final Widget view) {
        assert child != null && view != null && container.children.contains(child);

        int index = relativePosition(child, container.children, children);

        child.installMargins(view);

        // if panel is inside linear container, and there are several dynamic components fixing tab flex basis to avoid
        boolean fixFlexBasis = view instanceof FlexTabbedPanel && child.getFlex() > 0 && container.getFlexCount() > 1;
        if(fixFlexBasis) {
            FlexTabbedPanel tabbedView = (FlexTabbedPanel) view;
            tabbedView.setBeforeSelectionHandler(tabIndex -> {
                if(tabIndex > 0)
                    tabbedView.fixFlexBasis(vertical);
            });
        }

        children.add(index, child);
        childrenViews.add(index, wrapAndOverflowView(child, view, fixFlexBasis));

        addImpl(index, child, view);

        if(child.autoSize && view instanceof GAbstractTableController.GridPanel)
            addUpdateLayoutListener(requestIndex -> ((GAbstractTableController.GridPanel)view).autoSize());
    }

    private Widget wrapAndOverflowView(GComponent child, Widget view, boolean fixFlexBasis) {
        // border should be used by linear container (just like tab does)

        // stretch means flex : 1, with basis either 0 (default in css - 1 1 0), either auto (1 0 auto, what we want to get if we want to "push scroll" to the upper container)

        // overflow can be set only for both directions
        //
        // overflow cannot be visible in only one direction (
        //      https://stackoverLflow.com/questions/6421966/css-overflow-x-visible-and-overflow-y-hidden-causing-scrollbar-issue
        //      Short Version:
        //      If you are using visible for either overflow-x or overflow-y and something other than visible for the other, the visible value is interpreted as auto. )

        //<div style="display:flex; flex-direction:column; position:absolute; left:0; top:0; right:500px; bottom:300px; overflow:auto;">
        //  <div style="background: green; flex: 1 0 0px; min-height:0px; align-self:stretch; display:flex; flex-direction:row;">
        //    <div style="background: yellow; flex: 1 0 auto; align-self:stretch; overflow:auto;">
        //      <pre>B
        //        C                                                                G
        //        D
        //                E
        //        F
        //        G</pre>
        //   </div>
        //  </div>
        //  <div style="flex:0 0 auto; align-self:stretch;">
        //     A
        //  </div>
        //</div>

        // in theory it should look like
        //
        // MAIN direction:
        // if auto size (basis != auto)
        //      overflow : any (auto or visible)
        // else
        //      overflow : auto
        //
        // OPPOSITE direction:
        //
        // if auto size
        //    if stretch
        //      overflow: visible (which is default)
        //          however it cannot be combined with auto, so if auto is used for main direction (basis != auto) we should wrap into:
        //              flexpanel with opposite direction and flex-basis auto and :
        //                      overflow: auto set for the view (see upper example), visible for this flex panel
        //    else
        //      overflow: any (auto or visible)
        // else
        //      overflow: auto

        boolean isAutoSized = child.getSize(vertical) == null;
        boolean isOppositeAutoSized = child.getSize(!vertical) == null;

        if(!(isOppositeAutoSized && isAutoSized) || fixFlexBasis) // child is tab, since basis is fixed, strictly speaking this all check is an optimization
            view.getElement().getStyle().setOverflow(Style.Overflow.AUTO);

        // we need to do "caption wrapping" before auto size wrap since we want border to wrap all auto sized container
        // for auto size wrapping (below) - vertical direction and 0 (not auto (!)) flex-basis OR !vertical direction and auto (not 0 !) flex-basis will do for auto size (overflow:auto can be for panel and view itself)
        // for no auto size wrapping - either we need flex-basis 0 (not auto (!)) (any direction) and overflow:auto for a view, or flex-basis any (any direction) and overflow:auto for caption panel (not view)
        // we don't want to set flex-basis to 0 (since it can influence on parent container auto sizes), so we try to use strategies where flex-basis:auto is set

        // plus it's important to have auto for the view and not the flexcaptionPanel (since we don't want it to be scrolled), so there is one option left, with the same direction and 0 (or auto basis)
        FlexPanel wrapPanel = wrapBorderImpl(child);
        if(wrapPanel != null) {
            // one of the main problem is that stretch (opposite direction) can give you flex-basis 0, and "appropriate" auto size, but with flex (main direction) you can not obtain this effect (actually we can by setting shrink !!!!), so we have to look at size
            // now since we have auto size we'll use the option without shrink
            wrapPanel.addFillFlex(view, isAutoSized ? null : 0); // we need zero basis, because overflow:auto is set for a view (not for this panel), and with auto size view will overflow this captionpanel
            // wrapPanel.addFillStretch could also be used in theory
            view = wrapPanel;
        }

        // (!isAutoSized || hasCaption) // conflict since we want visible for one direction and auto for another (or we want to stretch caption for the whole container)
        if(isOppositeAutoSized && child.isStretch()) {
            wrapPanel = new FlexPanel(!vertical); // this container will have upper container size, but since the default overflow is visible, inner component with overflow:auto
            wrapPanel.setStyleName("oppositeStretchAutoSizePanel"); // just to identify this div in dom
            wrapPanel.addFillFlex(view, null);
            view = wrapPanel;
        }

        return view;
    }

    public void remove(GComponent child) {
        int index = children.indexOf(child);

        removeImpl(index, child);

        children.remove(index);
        childrenViews.remove(index);
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

    public abstract void updateCaption(GContainer container);

    public Widget getChildView(GComponent child) {
        int index = children.indexOf(child);
        return index != -1 ? childrenViews.get(index) : null;
    }

    protected Dimension getChildMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews, int childIndex) {
        GComponent child = children.get(childIndex);
        Widget childView = childrenViews.get(childIndex);
        Dimension dimensions = child instanceof GContainer
                               ? getMaxPreferredSize((GContainer) child, containerViews, true)
                               : getMaxPreferredSize(child, childView);
        Dimension result = enlargeDimension(dimensions, child.getHorizontalMargin(), child.getVerticalMargin());
        GFormLayout.setDebugDimensionsAttributes(childView, result);
        return result;
    }

    public static Dimension getMaxPreferredSize(GContainer child, Map<GContainer, GAbstractContainerView> containerViews, boolean max) {
        return overrideSize(child, containerViews.get(child).getMaxPreferredSize(containerViews), max);
    }
    private static Dimension getMaxPreferredSize(GComponent child, Widget childView) {
        return overrideSize(child, calculateMaxPreferredSize(childView), true);        
    }

    // with max (default) overriding there is a problem, that auto sizes can be not proportional to explicit sizes, so in fact one of the components can get smaller size (and thus scrolling)
    private static Dimension overrideSize(GComponent child, Dimension dimension, boolean max) {
        int childHeight = child.height;
        int childWidth = child.width;
        if(childHeight == -1 && childWidth == -1) // оптимизация
            return dimension;

        int preferredWidth = childWidth;
        if(preferredWidth == -1)
            preferredWidth = dimension.width;
        else if(max)
            preferredWidth = Math.max(preferredWidth, dimension.width);

        int preferredHeight = childHeight;
        if(preferredHeight == -1)
            preferredHeight = dimension.height;
        else if(max)
            preferredHeight = Math.max(preferredHeight, dimension.height);
        return new Dimension(preferredWidth, preferredHeight);
    }

    protected abstract Dimension getMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews);

    public interface UpdateLayoutListener {
        void updateLayout(long requestIndex);
    }

    private List<UpdateLayoutListener> updateLayoutListeners = new ArrayList<>();
    public void addUpdateLayoutListener(UpdateLayoutListener listener) {
        updateLayoutListeners.add(listener);
    }
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        for(UpdateLayoutListener updateLayoutListener : updateLayoutListeners)
            updateLayoutListener.updateLayout(requestIndex);
    }

    public static void add(FlexPanel panel, Widget widget, GComponent component, int beforeIndex) {
        boolean vertical = panel.isVertical();
        panel.add(widget, beforeIndex, component.getAlignment(), component.getFlex(), component.getSize(vertical));

        Integer crossSize = component.getSize(!vertical);
        boolean isStretch = component.isStretch();
        if(isStretch && crossSize != null && crossSize.equals(0)) // for opposite direction and stretch zero does not make any sense (it is zero by default)
            crossSize = null;
        FlexPanel.setBaseSize(widget, !vertical, crossSize, !isStretch);
    }

    protected abstract void addImpl(int index, GComponent child, Widget view);
    protected abstract FlexPanel wrapBorderImpl(GComponent child);
    protected abstract void removeImpl(int index, GComponent child);
    public abstract Widget getView();
}
