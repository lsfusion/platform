package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;

import static lsfusion.gwt.client.base.GwtClientUtils.calculateStackMaxPreferredSize;

// выполняет роль JComponentPanel в desktop
public class FlexPanel extends ComplexPanel implements RequiresResize, ProvidesResize, HasMaxPreferredSize {

    private static FlexPanelImpl impl = FlexPanelImpl.get();

    private final DivElement parentElement;

    private final boolean vertical;

    private final Justify justify;

    private boolean visible = true;

    public FlexPanel() {
        this(Justify.START);
    }

    public FlexPanel(Justify justify) {
        this(false, justify);
    }

    public FlexPanel(boolean vertical) {
        this(vertical, Justify.START);
    }

    public FlexPanel(boolean vertical, Justify justify) {
        this.vertical = vertical;
        this.justify = justify;

        parentElement = Document.get().createDivElement();

        impl.setupParentDiv(parentElement, vertical, justify);

        setElement(parentElement);
    }

    public Justify getJustify() {
        return justify;
    }

    public boolean isVertical() {
        return vertical;
    }

    public boolean isHorizontal() {
        return !isVertical();
    }

    @Override
    public void setVisible(boolean nVisible) {
        if (visible != nVisible) {
            visible = nVisible;
            impl.setVisible(parentElement, visible);
        }
    }

    @Override
    public void add(Widget child) {
        add(child, GFlexAlignment.START, 0);
    }
    
    public void addCentered(Widget child) {
        add(child, GFlexAlignment.CENTER, 0);
    }

    public void addStretched(Widget child) {
        add(child, GFlexAlignment.STRETCH, 0);
    }

    public void add(Widget child, int beforeIndex) {
        add(child, beforeIndex, GFlexAlignment.START, 0);
    }

    public void add(Widget child, GFlexAlignment alignment) {
        add(child, alignment, 0);
    }

    public void add(Widget widget, GFlexAlignment alignment, double flex) {
        add(widget, getWidgetCount(), alignment, flex);
    }

    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex) {
        add(widget, beforeIndex, alignment, flex, null);
    }

    public void addFill(Widget widget) {
        addFill(widget, getWidgetCount());
    }

    public void addFillFlex(Widget widget, Integer flexBasis) {
        addFill(widget, getWidgetCount(), flexBasis);
    }

    public void addFill(Widget widget, int beforeIndex) {
        addFill(widget, beforeIndex, null);
    }

    public void addFill(Widget widget, int beforeIndex, Integer flexBasis) {
        add(widget, beforeIndex, GFlexAlignment.STRETCH, 1, flexBasis);
    }

    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex, Integer flexBasis) {
        add(widget, beforeIndex, alignment, flex, flexBasis, null);    
    }
    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex, Integer flexBasis, Integer crossAxisSize) {
        // Detach new child.
        widget.removeFromParent();

        // Logical attach.
        getChildren().insert(widget, beforeIndex);

        // Physical attach.
        Element childElement = widget.getElement();

        LayoutData layoutData = impl.insertChild(parentElement, childElement, beforeIndex, alignment, flex, flexBasis);
        widget.setLayoutData(layoutData);

        setBaseSize(widget, vertical ? crossAxisSize : flexBasis, vertical ? flexBasis : crossAxisSize);

        // Adopt.
        adopt(widget);
    }

    // we're setting min-width/height and not width/height for two reasons:
    // a) alignment STRETCH doesn't work when width is set
    // b) flexBasis auto doesn't respect flexBasis of its descendants, but respects min-width (however with that approach in future there might be some problems with flex-shrink if we we'll want to support it)
    public static void setBaseSize(Widget widget, Integer width, Integer height) {
        if(width != null)
            widget.getElement().getStyle().setProperty("minWidth", width + "px");
        if(height != null)
            widget.getElement().getStyle().setProperty("minHeight", height + "px");
    }

    public void setChildFlex(Widget w, double flex) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            impl.setFlex((LayoutData) w.getLayoutData(), w.getElement(), flex);
        }
    }

    public void setChildFlex(Widget w, double flex, Integer flexBasis) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            impl.setFlex((LayoutData) w.getLayoutData(), w.getElement(), flex, flexBasis);
        }
    }

    public void setChildFlexBasis(Widget w, Integer flexBasis) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            impl.setFlexBasis((LayoutData) w.getLayoutData(), w.getElement(), flexBasis);
        }
    }

    public void fixFlexBasis(FixFlexBasisComposite w) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            impl.fixFlexBasis((LayoutData) w.getLayoutData(), w, vertical);
        }
    }

    public int getFlexBasis(Widget w) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            return impl.getFlexBasis((LayoutData) w.getLayoutData(), w.getElement(), vertical);
        }
        return -1;
    }

    public void setChildAlignment(Widget w, GFlexAlignment alignment) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            impl.setAlignment((LayoutData) w.getLayoutData(), w.getElement(), alignment);
        }
    }

    public double getChildFlex(Widget child) {
        assert child.getParent() == this;
        return ((LayoutData)child.getLayoutData()).flex;
    }

    public GFlexAlignment getChildAlignment(Widget child) {
        assert child.getParent() == this;
        return ((LayoutData)child.getLayoutData()).alignment;
    }

    @Override
    public boolean remove(Widget w) {
        boolean removed = super.remove(w);
        if (removed) {
            impl.removeChild((LayoutData) w.getLayoutData());
        }
        return removed;
    }

    @Override
    public void onResize() {
        if (!visible) {
            return;
        }
        for (Widget child : getChildren()) {
            if (child instanceof RequiresResize) {
                ((RequiresResize) child).onResize();
            }
        }
    }

    @Override
    public Dimension getMaxPreferredSize() {
        return calculateStackMaxPreferredSize(this.iterator(), isVertical());
    }

    public enum Justify {
        START, CENTER, END
    }

    public static final class LayoutData {
        public Element child;
        public GFlexAlignment alignment;
        public double flex;
        public Integer flexBasis;
//        public String flexBasis;

        public LayoutData(Element child, GFlexAlignment alignment, double flex, Integer flexBasis) {
            this.child = child;
            this.alignment = alignment;
            this.flex = flex;
            this.flexBasis = flexBasis;
        }
    }
}
