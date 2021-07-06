package lsfusion.gwt.client.form.design;

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

    public Integer getSize(boolean vertical) {
        int size = vertical ? height : width;
        if (size != -1)
            return size;
        return null;
    }

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

    public boolean isStretch() {
        return getAlignment() == GFlexAlignment.STRETCH;
    }
    public GFlexAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(GFlexAlignment alignment) {
        this.alignment = alignment;
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

    public void installMargins(Widget widget) {
        if(hasMargins()) // margins and not padding to not override other paddings
            GwtClientUtils.installMargins(widget.getElement(), marginTop, marginBottom, marginLeft, marginRight);
//        GwtClientUtils.installPaddings(widget.getElement(), marginTop, marginBottom, marginLeft, marginRight);
    }
}