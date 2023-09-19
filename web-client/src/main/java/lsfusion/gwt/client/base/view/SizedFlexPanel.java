package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.view.MainFrame;

// extended flex panel with alignShrink, and preferred size support
public class SizedFlexPanel extends FlexPanel {

    public SizedFlexPanel(boolean vertical, GFlexAlignment flexAlignment, GridLines gridLines, boolean wrap, Boolean resizeOverflow) {
        super(vertical, flexAlignment, gridLines, wrap, resizeOverflow);
    }

    public SizedFlexPanel(boolean vertical) {
        super(vertical);
    }

    public SizedFlexPanel() {
    }

    protected void addFillShrinkSized(Widget widget) {
//        addFillShrink(widget);
        addSized(widget, getWidgetCount(), 1, true, null, GFlexAlignment.STRETCH, true, null);
//         addSized(widget, getWidgetCount(), 1, false, null, GFlexAlignment.STRETCH, false, null);
    }

// test for the intrinistic height
//    <div style="display:flex;height:100px;background-color:blue">
//  <div style="">
//  A
//  </div>
//  <div style="align-self:flex-start;overflow:auto;max-height:-webkit-fill-available">
//  <div style="height:200px;width:200px;background-color:red;align-self:stretch">
//  B
//  </div>
//  </div>
//  </div>

    public void addSized(Widget widget, int beforeIndex, double flex, boolean shrink, GSize size, GFlexAlignment alignment, boolean alignShrink, GSize alignSize) {
        boolean vertical = isVertical();
        Element element = widget.getElement();

        boolean supportsIntrinisticHeight = !MainFrame.firefox;

        boolean isStretch = alignment == GFlexAlignment.STRETCH;

        // we could save one more container (by not creating wrap, but changing widget if is already flexpanel with a simple structure, collapsablepanel, tab wrapper)
        if((isStretch && !wrap) != alignShrink || // incorrect opposite shrink, stretch with nowrap automatically shrinks, in other cases nothing shrinks
                isStretch && alignSize != null) { // incorrect opposite size, if size is defined, alignment stretch is ignored
            boolean fixed = true;
            if((alignShrink || alignSize != null) && (vertical || supportsIntrinisticHeight)) {
                setIntrinisticSize(element, !vertical, alignShrink, isStretch && !wrap, alignSize);
            } else {
                if(vertical) {
                    assert isStretch;
                    assert alignSize == null;
                    setMinPanelWidth(element, "fit-content");
                } else { // for height fit-content doesn't work
                    if(isStretch && wrap && alignShrink && alignSize == null) {
                        // actually opposite flex panel won't help in this case (because opposite flex panel won't be shrinked with STRETCH either)
                        assert !supportsIntrinisticHeight;
                    } else
                        fixed = false;
               }
            }

            if(!fixed) {
                FlexPanel wrapPanel = new FlexPanel(!vertical, isStretch ? GFlexAlignment.START : alignment);
                wrapPanel.transparentResize = true;
                wrapPanel.addStyleName("oppositeSizeCssFixPanel"); // just to identify this div in dom
                wrapPanel.add(widget, GFlexAlignment.STRETCH, isStretch ? 1 : 0, alignShrink, alignSize);
                if (size != null) { // we want to use intristic widths, because otherwise (when setting the size to the wrap panel) margin/border/padding will be ignored
                    assert !vertical || supportsIntrinisticHeight;
                    setIntrinisticSize(element, vertical, true, true, size);
                    setPanelSize(element, vertical, size);
                    size = null;
                }

                widget = wrapPanel;
                element = widget.getElement();
                alignment = GFlexAlignment.STRETCH;
                alignSize = null;
            }
        }

        setPanelSize(element, !vertical, alignSize);

        add(widget, beforeIndex, alignment, flex, shrink, size);
    }

    private static void setIntrinisticSize(Element element, boolean vertical, boolean isShrink, boolean isStretch, GSize size) {
        element.setPropertyObject(vertical ? "intrinisticShrinkHeight" : "intrinisticShrinkWidth", size);

        if(isShrink) {
            if (vertical)
                element.addClassName("intr-shrink-height");
            else
                element.addClassName("intr-shrink-width");
        }

        if (isStretch) {
            if(vertical)
                element.addClassName("intr-stretch-height");
            else
                element.addClassName("intr-stretch-width");
        }
    }

    protected static void setIntrinisticPreferredSize(boolean set, boolean vertical, Widget widget) {
        Element element = widget.getElement();
        GSize intrinisticShrinkSize = (GSize) element.getPropertyObject(vertical ? "intrinisticShrinkHeight" : "intrinisticShrinkWidth");
        if(intrinisticShrinkSize != null) {
            if (set) {
                if(vertical)
                    element.removeClassName("intr-shrink-height");
                else
                    element.removeClassName("intr-shrink-width");
            } else {
                if(vertical)
                    element.addClassName("intr-shrink-height");
                else
                    element.addClassName("intr-shrink-width");
            }
            FlexPanel.setPanelSize(element, vertical, set ? null : intrinisticShrinkSize);
            FlexPanel.setMinPanelSize(element, vertical, set ? intrinisticShrinkSize : null);
        }
    }

    public void removeSized(Widget widget) {
        if(widget.getParent() != this)
            widget = widget.getParent();
        super.remove(widget);
    }

    public void removeSized(int index) {
        super.remove(getWidget(index));
    }

    @Override
    public int getWidgetIndex(Widget child) {
        assert false;
        return super.getWidgetIndex(child);
    }

    @Override
    public boolean remove(Widget w) {
        assert false;
        return super.remove(w);
    }
}
