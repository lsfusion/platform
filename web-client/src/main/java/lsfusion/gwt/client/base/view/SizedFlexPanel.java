package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import org.springframework.security.core.parameters.P;

// extended flex panel with alignShrink, and preferred size support
public class SizedFlexPanel extends FlexPanel {

    public SizedFlexPanel(boolean vertical, GFlexAlignment flexAlignment, GridLines gridLines, boolean wrap) {
        super(vertical, flexAlignment, gridLines, wrap);
    }

    public SizedFlexPanel(boolean vertical, GFlexAlignment flexAlignment) {
        super(vertical, flexAlignment);
    }

    public SizedFlexPanel(boolean vertical) {
        super(vertical);
    }

    public SizedFlexPanel() {
    }

    public void addSized(Widget widget, int beforeIndex, double flex, boolean shrink, GSize size, GFlexAlignment alignment, boolean alignShrink, GSize alignSize) {
        boolean vertical = isVertical();
        Element element = widget.getElement();

        boolean isStretch = alignment == GFlexAlignment.STRETCH;

        boolean incorrectOppositeShrink = isStretch != alignShrink; // if size is 0 we don't care about the shrink
        boolean incorrectOppositeSize = isStretch && alignSize != null; // if size is definite, alignment stretch is ignored

        // we could save one more container (by not creating wrap, but changing widget if is already flexpanel with a simple structure, collapsablepanel, tab wrapper)
        if(incorrectOppositeShrink || incorrectOppositeSize) {
            boolean fixed;
            if(vertical) {
                fixed = true;
                if(alignShrink)
                    setIntristicWidths(element, isStretch);
                else {
                    assert isStretch;
                    if(alignSize == null) {
                        setMinWidth(element, "fit-content");
                    } else {
                        // in theory max(fill-available, alignSize) should be, but fill-available cannot be used in calcs, so
                        if(size == null) // we're using an extra div
                            fixed = false;
                        else // we're fucked so will use 100% (it ignores margins, but you gonna do)
                            setMinWidth(element, "calc(max(100%," + alignSize.getString() + "))");
                    }
                }
            } else // for height nothing of the above works (even fill-content)
                fixed = false;

            if(!fixed) {
                FlexPanel wrapPanel = new FlexPanel(!vertical);
                wrapPanel.setStyleName("oppositeSizeCssFixPanel"); // just to identify this div in dom
                wrapPanel.add(widget, GFlexAlignment.STRETCH, isStretch ? 1 : 0, alignShrink, alignSize);
                if (!isStretch)
                    wrapPanel.setFlexAlignment(alignment);
                if (size != null) { // we want to use intristic widths, because otherwise (when setting the size to the wrap panel) margin/border/padding will be ignored
                    assert !vertical;
                    setIntristicWidths(element, true);
                    setWidth(element, size);
                    size = null;
                }

                widget = wrapPanel;
                element = widget.getElement();
                alignment = GFlexAlignment.STRETCH;
                alignSize = null;
            }
        }

        setSize(element, !vertical, alignSize);

        add(widget, beforeIndex, alignment, flex, shrink, size);
    }

    private static void setIntristicWidths(Element element, boolean isStretch) {
//        setMaxWidth(element, "fill-available");
        element.addClassName("shrink-width");

        if(isStretch)
            element.addClassName("stretch-width");
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
