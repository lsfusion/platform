package platform.gwt.view2;
import java.util.ArrayList;

public class GContainer extends GComponent {
    public ArrayList<GComponent> children = new ArrayList<GComponent>();
    public String title;
    public String description;
    public GContainerType type;
    public boolean isLayout;
    public boolean isVertical;
    public boolean resizable;

    public enum GwtContainer {
        VLAYOUT,

    }

    @Override
    public String toString() {
        return "GContainer" +
                "[" + sID + "]" +
                "[" + type + "]{" +
                "title='" + title + '\'' +
                ", isLayout=" + isLayout +
                ", isVertical=" + isVertical +
                ", hAlign=" + hAlign +
                ", resizable=" + resizable +
                '}';
    }

    public static String[] getTypeNamesList() {
        return new String[]{"CONTAINER", "TABBED PANE", "SPLIT PANE VERTICAL", "SPLIT PANE HORIZONTAL"};
    }
}
