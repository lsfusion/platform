package platform.gwt.form2.shared.view;
import java.util.ArrayList;
import java.util.List;

public class GContainer extends GComponent {
    public ArrayList<GComponent> children = new ArrayList<GComponent>();
    public String title;
    public String description;
    public GContainerType type;
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
                ", isVertical=" + isVertical +
                ", hAlign=" + hAlign +
                ", resizable=" + resizable +
                '}';
    }

    public static String[] getTypeNamesList() {
        return new String[]{"CONTAINER", "TABBED PANE", "SPLIT PANE VERTICAL", "SPLIT PANE HORIZONTAL"};
    }

    public boolean hasSingleGridInTree() {
        List<GGrid> grids = getAllGrids();
        return grids.size() == 1 && (grids.get(0).groupObject.isRecursive || grids.get(0).groupObject.parent != null);
    }

    public List<GGrid> getAllGrids() {
        List<GGrid> grids = new ArrayList<GGrid>();
        for (GComponent child : children) {
            if (child instanceof GGrid) {
                grids.add((GGrid) child);
            } else if (child instanceof GContainer) {
                grids.addAll(((GContainer) child).getAllGrids());
            }
        }
        return grids;
    }

    public boolean containsTreeGroup() {
        for (GComponent child : children) {
            if (child instanceof GTreeGroup) {
                return true;
            } else if (child instanceof GContainer) {
                boolean result = ((GContainer) child).containsTreeGroup();
                if (result) {
                    return result;
                }
            }
        }
        return false;
    }
}
