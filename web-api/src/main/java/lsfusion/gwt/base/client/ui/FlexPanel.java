package lsfusion.gwt.base.client.ui;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;

import static lsfusion.gwt.base.client.GwtClientUtils.calculateStackPreferredSize;

/**
 * Browser support: http://caniuse.com/flexbox
 */
public class FlexPanel extends ComplexPanel implements RequiresResize, ProvidesResize, HasPreferredSize {

    private static FlexPanelImpl impl = FlexPanelImpl.get();

    private final DivElement parentElement;

    private final boolean vertical;

    private final Justify justify;

    private boolean visible = true;

    public FlexPanel() {
        this(Justify.LEADING);
    }

    public FlexPanel(Justify justify) {
        this(false, justify);
    }

    public FlexPanel(boolean vertical) {
        this(vertical, Justify.LEADING);
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
        add(child, GFlexAlignment.LEADING, 0);
    }
    
    public void addCentered(Widget child) {
        add(child, GFlexAlignment.CENTER, 0);
    }

    public void addStretched(Widget child) {
        add(child, GFlexAlignment.STRETCH, 0);
    }

    public void add(Widget child, int beforeIndex) {
        add(child, beforeIndex, GFlexAlignment.LEADING, 0);
    }

    public void add(Widget child, GFlexAlignment alignment) {
        add(child, alignment, 0);
    }

    public void add(Widget widget, GFlexAlignment alignment, double flex) {
        add(widget, getWidgetCount(), alignment, flex);
    }

    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex) {
        add(widget, beforeIndex, alignment, flex, "auto");
    }

    public void add(Widget widget, GFlexAlignment alignment, double flex, String flexBasis) {
        add(widget, getWidgetCount(), alignment, flex, flexBasis);
    }

    public void addFill(Widget widget) {
        addFill(widget, getWidgetCount());
    }

    public void addFill(Widget widget, String flexBasis) {
        addFill(widget, getWidgetCount(), flexBasis);
    }

    public void addFill(Widget widget, int beforeIndex) {
        addFill(widget, beforeIndex, "auto");
    }

    public void addFill(Widget widget, int beforeIndex, String flexBasis) {
        add(widget, beforeIndex, GFlexAlignment.STRETCH, 1, flexBasis);
    }

    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex, String flexBasis) {
        // Detach new child.
        widget.removeFromParent();

        // Logical attach.
        getChildren().insert(widget, beforeIndex);

        // Physical attach.
        Element childElement = widget.getElement();

        LayoutData layoutData = impl.insertChild(parentElement, childElement, beforeIndex, alignment, flex, flexBasis);
        widget.setLayoutData(layoutData);

        // Adopt.
        adopt(widget);
    }

    public void setChildConstraints(Widget w, GFlexAlignment alignment, double flex, String flexBasis) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            LayoutData layoutData = (LayoutData) w.getLayoutData();
            Element childElement = w.getElement();
            impl.setFlex(layoutData, childElement, flex, flexBasis);
            impl.setAlignment(layoutData, childElement, alignment);
        }
    }

    public void setChildFlex(Widget w, double flex) {
        setChildFlex(w, flex, "auto");
    }

    public void setChildFlex(Widget w, double flex, String flexBasis) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            impl.setFlex((LayoutData) w.getLayoutData(), w.getElement(), flex, flexBasis);
        }
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
    public Dimension getPreferredSize() {
        return calculateStackPreferredSize(this.iterator(), isVertical());
    }

    public static enum Justify {
        LEADING, CENTER, TRAILING
    }

    public static final class LayoutData {
        Element child;
        GFlexAlignment alignment;
        double flex;
        String flexBasis;

        public LayoutData(Element child, GFlexAlignment alignment, double flex, String flexBasis) {
            this.child = child;
            this.alignment = alignment;
            this.flex = flex;
            this.flexBasis = flexBasis;
        }
    }
}
