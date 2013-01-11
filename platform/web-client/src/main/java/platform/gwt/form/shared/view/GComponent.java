package platform.gwt.form.shared.view;

import platform.gwt.form.shared.view.changes.dto.ColorDTO;

import java.io.Serializable;

public class GComponent implements Serializable {
    public int ID;
    public String sID;
    public GContainer container;
    public boolean defaultComponent;

    public double fillHorizontal = -1;
    public double fillVertical = -1;

    public int minimumWidth = -1;
    public int minimumHeight = -1;
    public int maximumWidth = -1;
    public int maximumHeight = -1;
    public int preferredWidth = -1;
    public int preferredHeight = -1;

    public Alignment hAlign;

    public ColorDTO background;
    public ColorDTO foreground;

    public Integer fontSize;
    public String fontFamily;

    public enum Alignment {
        LEFT, RIGHT, CENTER
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
}