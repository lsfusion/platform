package platform.gwt.view2;

import java.io.Serializable;

public class GComponent implements Serializable {
    public int ID;
    public String sID;
    public GContainer container;
    public boolean defaultComponent;
    public boolean drawToToolbar;

    @Override
    public String toString() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        return className + "{" +
                "sID='" + sID + '\'' +
                ", defaultComponent=" + defaultComponent +
                ", drawToToolbar=" + drawToToolbar +
                '}';
    }
}