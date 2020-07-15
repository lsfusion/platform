package lsfusion.gwt.client.form.design;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import java.io.Serializable;

public class GComponent implements Serializable {
    public int ID;
    public String sID;
    public GContainer container;
    public boolean defaultComponent;

    public int width = -1;
    public int height = -1;
    
    public boolean autoSize;

    protected double flex = 0;
    protected GFlexAlignment alignment;

    public int marginTop;
    public int marginBottom;
    public int marginLeft;
    public int marginRight;

    public ColorDTO background;
    public ColorDTO foreground;

    public GFont font;
    public GFont captionFont;

    @Override
    public String toString() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        return className + "{" +
               "sID='" + sID + '\'' +
               ", defaultComponent=" + defaultComponent +
               '}';
    }

    public boolean isTab() {
        return container != null && container.isTabbed();
    }

    public double getFlex() {
        return flex;
    }

    public void setFlex(double flex) {
        this.flex = flex;
    }

    public GFlexAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(GFlexAlignment alignment) {
        this.alignment = alignment;
    }

    public boolean isVerticallyStretched() {
        return isStretched(container.isLinearVertical(), container.isLinearHorizontal());
    }

    public boolean isStretched(boolean linearVertical, boolean linearHorizontal) {
        if (container != null) {
            if (container.isScroll() || container.isTabbed() || container.isSplit()) {
                return true;
            } else if (container.isColumns()) {
                return false;
            }

            assert container.isLinear();
            return linearVertical && getFlex() > 0 || linearHorizontal && getAlignment() == GFlexAlignment.STRETCH;
        }
        return false;
    }

    public boolean isHorizontallyStretched() {
        return isStretched(container.isLinearHorizontal(), container.isLinearVertical());
    }

    public boolean hasMargins() {
        return marginTop != 0 || marginBottom != 0 || marginLeft != 0 || marginRight != 0;
    }

    public int getVerticalMargin() {
        return marginTop + marginBottom;
    }

    public int getHorizontalMargin() {
        return marginLeft + marginRight;
    }

    public int getMargins(boolean vertical) {
        return vertical ? getVerticalMargin() : getHorizontalMargin();
    }

    public void installPaddings(Widget widget) {
        installPaddings(widget.getElement());
    }

    public void installPaddings(Element element) {
        GwtClientUtils.installPaddings(element, marginTop, marginBottom, marginLeft, marginRight);
    }
}