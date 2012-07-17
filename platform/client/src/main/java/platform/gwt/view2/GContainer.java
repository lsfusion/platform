package platform.gwt.view2;
import java.util.ArrayList;

public class GContainer extends GComponent {
    public ArrayList<GComponent> children = new ArrayList<GComponent>();
    public String title;
    public String description;
    public GContainerType type;
    public boolean gwtIsLayout;
    public boolean gwtVertical;
    public Alignment hAlign;
    public boolean resizable;

    public enum GwtContainer {
        VLAYOUT,

    }

    public enum Alignment {
        LEFT, RIGHT, CENTER
    }

    @Override
    public String toString() {
        return "GContainer" +
                "[" + sID + "]" +
                "[" + type + "]{" +
                "title='" + title + '\'' +
                ", isLayout=" + gwtIsLayout +
                ", isVertical=" + gwtVertical +
                ", hAlign=" + hAlign +
                ", resizable=" + resizable +
                '}';
    }

    public static String[] getTypeNamesList() {
        return new String[]{"CONTAINER", "TABBED PANE", "SPLIT PANE VERTICAL", "SPLIT PANE HORIZONTAL"};
    }
}
