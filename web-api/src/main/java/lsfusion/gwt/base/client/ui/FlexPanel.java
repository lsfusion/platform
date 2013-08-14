package lsfusion.gwt.base.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

/**
 * Browser support: http://caniuse.com/flexbox
 */
public class FlexPanel extends ComplexPanel implements RequiresResize, ProvidesResize {

    private static FlexPanelImpl impl = GWT.create(FlexPanelImpl.class);

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
        // Detach new child.
        widget.removeFromParent();

        // Logical attach.
        getChildren().insert(widget, beforeIndex);

        // Physical attach.
        Element childElement = widget.getElement();

        LayoutData layoutData = impl.insertChild(parentElement, childElement, beforeIndex, alignment, flex);
        widget.setLayoutData(layoutData);

        // Adopt.
        adopt(widget);
        if (widget instanceof FlexAware) {
            ((FlexAware) widget).addedToFlexPanel(this, alignment, flex);
        }
    }

    public void setChildConstraints(Widget w, GFlexAlignment alignment, double flex) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            Element childElement = w.getElement();
            impl.setFlex(parentElement, childElement, index, flex);
            impl.setAlignment(parentElement, childElement, index, alignment);
        }
    }

    public void setChildFlex(Widget w, double flex) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            impl.setFlex(parentElement, w.getElement(), index, flex);
        }
    }

    public void setChildAlignment(Widget w, GFlexAlignment alignment) {
        int index = getWidgetIndex(w);
        if (index != -1) {
            impl.setAlignment(parentElement, w.getElement(), index, alignment);
        }
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
        for (Widget child : getChildren()) {
            if (child instanceof RequiresResize) {
                ((RequiresResize) child).onResize();
            }
        }
    }

    public static enum Justify {
        LEADING, CENTER, TRAILING
    }

    public static final class LayoutData {
        Element container;
        Element child;

        public LayoutData(Element container, Element child) {
            this.container = container;
            this.child = child;
        }
    }

    public static interface FlexAware {
        public void addedToFlexPanel(FlexPanel parent, GFlexAlignment alignment, double flex);
    }
}
