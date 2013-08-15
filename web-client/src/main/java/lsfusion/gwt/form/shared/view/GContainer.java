package lsfusion.gwt.form.shared.view;
import java.util.ArrayList;
import java.util.List;

public class GContainer extends GComponent {
    public ArrayList<GComponent> children = new ArrayList<GComponent>();
    public String caption;
    public String description;
    public GContainerType type;

    @Override
    public String toString() {
        return "GContainer" +
                "[" + sID + "]" +
                "[" + type + "]{" +
                "caption='" + caption + '\'' +
                ", hAlign=" + hAlign +
                '}';
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

    public String getChildPercentSize(GComponent child, boolean width) {
        assert children.contains(child);
        return (width ? child.fillHorizontal : child.fillVertical) / getChildFill(!width) * 100 + "%";
    }

    private double getChildFill(boolean vertical) {
        double fill = 0;
        boolean chooseMax = true;
        if (!vertical) {
            if (toFlow() || !drawVertical()) {
                chooseMax = false;
            }
        } else if (!toFlow() && drawVertical()) {
            chooseMax = false;
        }

        for (GComponent child : children) {
            double childFill = 0;
            if (vertical) {
                if (child.fillVertical > 0) {
                    childFill = child.fillVertical;
                }
            } else {
                if (child.fillHorizontal > 0) {
                    childFill = child.fillHorizontal;
                }
            }
            if (chooseMax) {
                fill = Math.max(fill, childFill);
            } else {
                fill += childFill;
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

    public List<GPropertyDraw> getAllPropertyDraws() {
        List<GPropertyDraw> draws = new ArrayList<GPropertyDraw>();
        for (GComponent child : children) {
            if (child instanceof GPropertyDraw) {
                draws.add((GPropertyDraw) child);
            } else if (child instanceof GContainer) {
                draws.addAll(((GContainer) child).getAllPropertyDraws());
            }
        }
        return draws;
    }

    public boolean containsTreeGroup() {
        return !getAllTreeGrids().isEmpty();
    }

    public boolean isNotOriented() {
        return type == GContainerType.CONTAINERVH || type == GContainerType.TABBED_PANEL;
    }

    public boolean isVertical() {
        return type == GContainerType.VERTICAL_SPLIT_PANEL || type == GContainerType.CONTAINERV;
    }

    public boolean drawVertical() {
        return isNotOriented() || isVertical();
    }

    public boolean toFlow() {
        return isNotOriented() && fillVertical <= 0 && containerFlows();
    }

    private boolean containerFlows() {
        if (container != null) {
            if (container.isNotOriented()) {
                return container.containerFlows();
            } else {
                if (container.isVertical()) {
                    return (container.fillVertical > 0 && !container.isGroupObjectWithBannedGrid()) || container.containerFlows();
                } else {
                    return (container.fillHorizontal > 0 && !container.isGroupObjectWithBannedGrid()) || container.containerFlows();
                }
            }
        }
        return false;
    }

    private boolean isGroupObjectWithBannedGrid() {
        List<GGrid> grids = getAllGrids();
        return grids.size() == 1 && grids.get(0).groupObject.banClassView.contains("GRID");
    }
}
