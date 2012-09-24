package platform.gwt.form2.shared.view;

import java.io.Serializable;

public class GComponent implements Serializable {
    public int ID;
    public String sID;
    public GContainer container;
    public boolean defaultComponent;
    public double fillHorizontal = -1;
    public double fillVertical = -1;
    public int prefferedWidth = -1;
    public int prefferedHeight = -1;
    public int absoluteWidth = -1;
    public int absoluteHeight = -1;
    public Alignment hAlign;

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
}