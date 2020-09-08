package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

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
        add(child, GFlexAlignment.START);
    }
    
    public void addCentered(Widget child) {
        add(child, GFlexAlignment.CENTER);
    }

    public void addStretched(Widget child) {
        add(child, GFlexAlignment.STRETCH);
    }

    public void add(Widget widget, GFlexAlignment alignment) {
        add(widget, getWidgetCount(), alignment);
    }

    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment) {
        add(widget, beforeIndex, alignment, 0, null); // maybe here it also makes sense to set basis to 0 as in addFill, but for now it's used mostly in vertical container for simple components
    }

    public void addFill(Widget widget) {
        addFill(widget, getWidgetCount());
    }

    public void addFillFlex(Widget widget, Integer flexBasis) {
        assert vertical;
        addFill(widget, getWidgetCount(), flexBasis);
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

    // it's tricky here
    // it seems that flex basis auto works different in vertical (column) and horizontal (row) directions
    // in horizontal direction it seems that flex basis does not respect descendants flex-basis, and in vertical direction it does
    // test case in jsfiddle (changing direction to column doesn't work)
    // <div style="overflow: hidden;display: flex;flex-direction: row; position:absolute; top:0; left:0; right:0; bottom:0;">
    //  <div style="overflow: hidden;display: flex;flex-direction: row;flex: 0 0 auto;">
    //     <div style="overflow: hidden;display: flex;flex-direction: row;flex: 1 0 0px;">
    //       <div>TEXT</div>
    //     </div>
    //  </div>
    //</div>
    // so for vertical direction we keep auto since it works predictable and as expected
    // for horizontal direction we set 0 - basis since it doesn't influence on other autos
    // for now all that is important only for autoSize props, but in theory can be important in other cases
    public void addFill(Widget widget, int beforeIndex) {
        addFill(widget, beforeIndex, vertical ? null : 0);
    }

    // we're setting min-width/height and not width/height for two reasons:
    // a) alignment STRETCH doesn't work when width is set
    // b) flexBasis auto doesn't respect flexBasis of its descendants (!!! it's not true for vertical direction, see addFill comment !!!), but respects min-width (however with that approach in future there might be some problems with flex-shrink if we we'll want to support it)
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

    public void fixFlexBasis(FlexTabbedPanel w) {
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
