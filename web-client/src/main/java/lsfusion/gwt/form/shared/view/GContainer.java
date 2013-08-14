package lsfusion.gwt.form.shared.view;

import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GAlignment;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.form.shared.view.GContainerType.*;

public class GContainer extends GComponent {
    public String caption;
    public String description;

    public GContainerType type;
    public GAlignment childrenAlignment;

    public int gapX;
    public int gapY;
    public int columns;

    public ArrayList<GComponent> children = new ArrayList<GComponent>();

    @Override
    public String toString() {
        return "GContainer" +
                "[" + sID + "]" +
                "[" + type + "]{" +
                "caption='" + caption + '\'' +
                ", alignment=" + alignment +
                '}';
    }

    public FlexPanel.Justify getFlexJustify() {
        switch (childrenAlignment) {
            case LEADING: return FlexPanel.Justify.LEADING;
            case CENTER: return FlexPanel.Justify.CENTER;
            case TRAILING: return FlexPanel.Justify.TRAILING;
        }
        throw new IllegalStateException("Unknown alignment");
    }

    public boolean isTabbed() {
        return type == TABBED_PANE;
    }

    public boolean isSplit() {
        return type == HORIZONTAL_SPLIT_PANE || type == VERTICAL_SPLIT_PANE;
    }

    public boolean isVerticalSplit() {
        return type == VERTICAL_SPLIT_PANE;
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

    public boolean isVertical() {
        return type == CONTAINERV;
    }

    public boolean isHorizontal() {
        return type == CONTAINERH;
    }

    public boolean isLinear() {
        return isVertical() || isHorizontal();
    }

    public boolean isColumns() {
        return type == COLUMNS;
    }
}
