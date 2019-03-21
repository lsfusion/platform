package lsfusion.gwt.client.base.ui;

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

        if (crossAxisSize != null && crossAxisSize >= 0) {
            if (vertical) {
                widget.setWidth(crossAxisSize + "px");
            } else {
                widget.setHeight(crossAxisSize + "px");
            }
        }

        // верхние контейнеры при расчёте flexBasis не учитывают flexBasis потомков, а используют для этого другие значения,
        // в частности minWidth. в результате, если у одного из верхних контейнеров выставлен flexBasis = auto, а у одного из потомков 
        // большое значение flexBasis, то этот потомок обрезается верхним контейнером, который рассчитал себе меньший размер.
        // пока у нас не предполагается, что компонент будет ужиматься меньше значения flexBasis
        // - такое решение будет мешать поддерживать flex-shrink
        // - на момент фикса данный случай прекрасно работал в IE (flexBasis потомка участвует в расчёте flexBasis родителя) 
        if (flexBasis != null)
            if (vertical) {
                childElement.getStyle().setProperty("minHeight", flexBasis + "px");
            } else {
                childElement.getStyle().setProperty("minWidth", flexBasis + "px");
            }

        // Adopt.
        adopt(widget);
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
