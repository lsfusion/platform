package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtSharedUtils.relativePosition;

public abstract class GAbstractContainerView {
    protected final GContainer container;

    protected final boolean vertical;

    protected final List<GComponent> children = new ArrayList<>();
    protected final List<SizedWidget> childrenViews = new ArrayList<>();

    protected final boolean alignCaptions;
    protected List<CaptionWidget> childrenCaptions;

    protected GAbstractContainerView(GContainer container) {
        this.container = container;

        vertical = container.isVertical();

        alignCaptions = container.isAlignCaptions();
        if(alignCaptions)
            childrenCaptions = new ArrayList<>();
    }

    public void add(GComponent child, final ComponentWidget view) {
        assert child != null && view != null && container.children.contains(child);

        int index = relativePosition(child, container.children, children);

//        child.installMargins(view);

        Widget widget = view.getWidget();

        // if panel is inside linear container, and there are several dynamic components fixing tab flex basis to avoid
        boolean fixFlexBasis = widget instanceof FlexTabbedPanel && child.isFlex() && container.getFlexCount() > 1;
        if(fixFlexBasis) {
            FlexTabbedPanel tabbedView = (FlexTabbedPanel) widget;
            tabbedView.setBeforeSelectionHandler(tabIndex -> {
                if(tabIndex > 0)
                    tabbedView.fixFlexBasis(vertical);
            });
        }

        children.add(index, child);
        childrenViews.add(index, view.widget.override(wrapAndOverflowView(child, widget, fixFlexBasis)));

        if(alignCaptions)
            childrenCaptions.add(index, view.caption);

        addImpl(index);
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

        boolean shrink = child.isShrink();
        boolean alignShrink = child.isAlignShrink();

        if(child.getWidth() != null || (!vertical ? shrink : alignShrink) || fixFlexBasis)
            view.getElement().addClassName("comp-shrink-horz");
        if(child.getHeight() != null || (vertical ? shrink : alignShrink) || fixFlexBasis)
            view.getElement().addClassName("comp-shrink-vert");

        // we need to do "caption wrapping" before auto size wrap since we want border to wrap all auto sized container
        // for auto size wrapping (below) - vertical direction and 0 (not auto (!)) flex-basis OR !vertical direction and auto (not 0 !) flex-basis will do for auto size (overflow:auto can be for panel and view itself)
        // for no auto size wrapping - either we need flex-basis 0 (not auto (!)) (any direction) and overflow:auto for a view, or flex-basis any (any direction) and overflow:auto for caption panel (not view)
        // we don't want to set flex-basis to 0 (since it can influence on parent container auto sizes), so we try to use strategies where flex-basis:auto is set

        // plus it's important to have auto for the view and not the flexcaptionPanel (since we don't want it to be scrolled), so there is one option left, with the same direction and 0 (or auto basis)

        FlexPanel wrapPanel = wrapBorderImpl(child);

        if(wrapPanel != null) {
            // 1 1 auto
            wrapPanel.addFillShrink(view);
            view = wrapPanel;
        }

        if(child.elementClass != null)
            view.getElement().addClassName(child.elementClass);

        return view;
    }

    public void remove(GComponent child) {
        int index = children.indexOf(child);

        removeImpl(index, child);

        if(alignCaptions)
            childrenCaptions.remove(index);

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

    public abstract void updateCaption(GContainer container);

    public Widget getChildView(GComponent child) {
        int index = children.indexOf(child);
        return index != -1 ? childrenViews.get(index).widget : null;
    }

    public interface UpdateLayoutListener {
        void updateLayout(long requestIndex);
    }

    private final List<UpdateLayoutListener> updateLayoutListeners = new ArrayList<>();
    public void addUpdateLayoutListener(UpdateLayoutListener listener) {
        updateLayoutListeners.add(listener);
    }
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        for(UpdateLayoutListener updateLayoutListener : updateLayoutListeners)
            updateLayoutListener.updateLayout(requestIndex);
    }

    protected Widget addChildrenWidget(SizedFlexPanel panel, int i, int beforeIndex) {
        SizedWidget widget = childrenViews.get(i);
        GComponent component = children.get(i);

        widget = widget.override(component.getWidth(), component.getHeight());

        widget.add(panel, beforeIndex, component.getFlex(), component.isShrink(), component.getAlignment(), component.isAlignShrink());

        return widget.widget;
    }

    protected abstract void addImpl(int index);
    protected abstract FlexPanel wrapBorderImpl(GComponent child);
    protected abstract void removeImpl(int index, GComponent child);
    public abstract Widget getView();
}
