package lsfusion.gwt.form.shared.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.shared.view.changes.dto.ColorDTO;

import java.io.Serializable;

public class GComponent implements Serializable {
    public int ID;
    public String sID;
    public GContainer container;
    public boolean defaultComponent;

    public int minimumWidth = -1;
    public int minimumHeight = -1;
    public int maximumWidth = -1;
    public int maximumHeight = -1;
    public int preferredWidth = -1;
    public int preferredHeight = -1;

    public double flex = 0;
    public GFlexAlignment alignment;

    public int marginTop;
    public int marginBottom;
    public int marginLeft;
    public int marginRight;

    public ColorDTO background;
    public ColorDTO foreground;

    public GFont font;
    public GFont headerFont;

    @Override
    public String toString() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        return className + "{" +
                "sID='" + sID + '\'' +
                ", defaultComponent=" + defaultComponent +
                '}';
    }

    public int getAbsoluteWidth() {
        if (preferredWidth == minimumHeight && preferredWidth == maximumWidth) {
            return preferredWidth;
        }
        return -1;
    }

    public int getAbsoluteHeight() {
        if (preferredHeight == minimumHeight && preferredHeight == maximumHeight) {
            return preferredHeight;
        }
        return -1;
    }

    public int getVerticalMargin() {
        return marginTop + marginBottom;
    }

    public int getHorizontalMargin() {
        return marginLeft + marginRight;
    }

    public void installMargins(Widget widget) {
        GwtClientUtils.installMargins(widget, marginTop, marginBottom, marginLeft, marginRight);
    }

    public Dimension getOffsetSize(Widget widget) {
        return GwtClientUtils.getOffsetSize(widget, getHorizontalMargin(), getVerticalMargin());
    }
}