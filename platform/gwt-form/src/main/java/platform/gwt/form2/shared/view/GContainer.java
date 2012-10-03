package platform.gwt.form2.shared.view;
import java.util.ArrayList;
import java.util.List;

public class GContainer extends GComponent {
    public ArrayList<GComponent> children = new ArrayList<GComponent>();
    public String title;
    public String description;
    public GContainerType type;
    public Boolean vertical;
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
                ", vertical=" + vertical +
                ", hAlign=" + hAlign +
                ", resizable=" + resizable +
                '}';
    }

    public static String[] getTypeNamesList() {
        return new String[]{"CONTAINER", "TABBED PANE", "SPLIT PANE VERTICAL", "SPLIT PANE HORIZONTAL"};
    }

    public void calculateFills() {
        for (GComponent child : children) {
            if (child instanceof GContainer) {
                ((GContainer) child).calculateFills();
            }
        }

        if (getAbsoluteWidth() == -1 && fillHorizontal < 0) {
            double childFill = getChildFill(false);
            if (childFill > 0) {
                fillHorizontal = childFill;
            }
        }

        if (getAbsoluteHeight() == -1 && fillVertical < 0) {
            double childFill = getChildFill(true);
            if (childFill > 0) {
                fillVertical = childFill;
            }
        }
    }

    private double getChildFill(boolean vertical) {
        double fill = 0;
        for (GComponent child : children) {
            if (vertical) {
                if (child.fillVertical > 0) {
                    fill += child.fillVertical;
                }
            } else {
                if (child.fillHorizontal > 0) {
                    fill += child.fillHorizontal;
                }
            }
        }
        return fill;
    }

    public boolean hasSingleGridInTree() {
        List<GGrid> grids = getAllGrids();
        return grids.size() == 1 && (grids.get(0).groupObject.isRecursive || grids.get(0).groupObject.parent != null) && !containsTreeGroup();
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

    public List<GTreeGroup> getAllTreeGrids() {
        List<GTreeGroup> grids = new ArrayList<GTreeGroup>();
        for (GComponent child : children) {
            if (child instanceof GTreeGroup) {
                grids.add((GTreeGroup) child);
            } else if (child instanceof GContainer) {
                grids.addAll(((GContainer) child).getAllTreeGrids());
            }
        }
        return grids;
    }

    public boolean containsTreeGroup() {
        return !getAllTreeGrids().isEmpty();
    }

    public boolean drawVertical() {
        return vertical == null || vertical;
    }

    public boolean toFlow() {
        if (container != null) {
            if (vertical == null) {
                if (fillVertical <= 0) {
                    if (container.vertical != null) {
                        if (container.vertical) {
                            return container.fillVertical > 0 || container.toFlow();
                        }
                    } else {
                        return container.toFlow();
                    }
                }
            } else {
                return !vertical && fillVertical <= 0;
            }
        }
        return false;
    }
}
