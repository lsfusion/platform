package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import java.util.ArrayList;
import java.util.List;

public abstract class GAbstractContainerView {
    protected final GContainer container;

    protected final boolean vertical;

    protected final List<GComponent> children = new ArrayList<>();
    protected final List<ComponentViewWidget> childrenViews = new ArrayList<>();

    protected List<CaptionWidget> childrenCaptions = new ArrayList<>();

    protected GAbstractContainerView(GContainer container) {
        this.container = container;

        vertical = container.isVertical();
    }

    public void add(GComponent child, final ComponentWidget view, ResizableComplexPanel attachContainer) {
        assert child != null && view != null && container.children.contains(child);

        int index = GwtSharedUtils.relativePosition(child, container.children, children);

//        child.installMargins(view);

        children.add(index, child);
        childrenCaptions.add(index, view.caption);

        ComponentViewWidget componentViewWidget = view.widget;
        SizedWidget singleWidget = componentViewWidget.getSingleWidget();
        if(singleWidget != null) {
            Widget widget = singleWidget.widget;
            // if panel is inside linear container, and there are several dynamic components fixing tab flex basis to avoid
            boolean fixFlexBasis = widget instanceof FlexTabbedPanel && child.isFlex() && container.getFlexCount() > 1;
            if (fixFlexBasis) {
                FlexTabbedPanel tabbedView = (FlexTabbedPanel) widget;
                tabbedView.setBeforeSelectionHandler(tabIndex -> {
                    if (tabIndex > 0)
                        tabbedView.fixFlexBasis(vertical);
                });
            }

            componentViewWidget = singleWidget.override(wrapAndOverflowView(index, widget, attachContainer, fixFlexBasis)).view;
        }

        childrenViews.add(index, componentViewWidget);

        addImpl(index);
    }

    private Widget wrapAndOverflowView(int index, Widget view, ResizableComplexPanel attachContainer, boolean fixFlexBasis) {
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

        GComponent child = children.get(index);
        boolean shrink = child.isShrink();
        boolean alignShrink = child.isAlignShrink();

        if((child.getWidth() != null || (!vertical ? shrink : alignShrink) || fixFlexBasis)) {
            GwtClientUtils.setupOverflowHorz(view.getElement(), child.getOverflowHorz());
        }
        if((child.getHeight() != null || (vertical ? shrink : alignShrink) || fixFlexBasis)) {
            GwtClientUtils.setupOverflowVert(view.getElement(), child.getOverflowVert());
        }

        if(child instanceof GContainer)
            GFormLayout.updateComponentClass(((GContainer) child).valueClass, view, "value");

        // we need to do "caption wrapping" before auto size wrap since we want border to wrap all auto sized container
        // for auto size wrapping (below) - vertical direction and 0 (not auto (!)) flex-basis OR !vertical direction and auto (not 0 !) flex-basis will do for auto size (overflow:auto can be for panel and view itself)
        // for no auto size wrapping - either we need flex-basis 0 (not auto (!)) (any direction) and overflow:auto for a view, or flex-basis any (any direction) and overflow:auto for caption panel (not view)
        // we don't want to set flex-basis to 0 (since it can influence on parent container auto sizes), so we try to use strategies where flex-basis:auto is set

        // plus it's important to have auto for the view and not the flexcaptionPanel (since we don't want it to be scrolled), so there is one option left, with the same direction and 0 (or auto basis)

        Widget wrapPanel = wrapBorderImpl(index);

        if(wrapPanel != null) {
            if(wrapPanel instanceof FlexPanel)
                ((FlexPanel)wrapPanel).addFillShrink(view);
            else { // popup
                attachContainer.add(view);
                ((PopupButton) wrapPanel).setContent(container, view);
            }
            view = wrapPanel;
        }

        GFormLayout.updateComponentClass(child.elementClass, view, BaseImage.emptyPostfix);

        return view;
    }

    public void remove(GComponent child) {
        int index = children.indexOf(child);

        removeImpl(index);

        children.remove(index);
        childrenCaptions.remove(index);
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

    public Widget getChildWidget(GComponent child) {
        int index = children.indexOf(child);
        if(index == -1)
            return null;

        SizedWidget singleWidget = getChildView(index).getSingleWidget();
        if(singleWidget != null)
            return singleWidget.widget;
        return null;
    }

    public ComponentViewWidget getChildView(int index) {
        return childrenViews.get(index);
    }

    protected int getChildPosition(int index) {
        int containerIndex;
        containerIndex = 0;
        for(int i = 0; i < index; i++)
            containerIndex += getChildView(i).getWidgetCount();
        return containerIndex;
    }

    public Widget getCaptionView(GComponent child) {
        int index = children.indexOf(child);
        return index != -1 ? childrenCaptions.get(index).widget.widget : null;
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
        ComponentViewWidget viewWidget = getChildView(i);
        GComponent component = children.get(i);

        viewWidget.add(panel, beforeIndex, component.getWidth(), component.getHeight(), component.getFlex(RendererType.PANEL), component.isShrink(), component.getAlignment(), component.isAlignShrink());

        SizedWidget singleWidget = viewWidget.getSingleWidget();
        return singleWidget != null ? singleWidget.widget : null;
    }

    protected void removeChildrenWidget(SizedFlexPanel panel, int i, int containerIndex) {
        ComponentViewWidget widget = getChildView(i);

        widget.remove(panel, containerIndex);
    }

    protected abstract void addImpl(int index);
    protected abstract Widget wrapBorderImpl(int index);
    protected abstract void removeImpl(int index);
    public abstract Widget getView();
}
