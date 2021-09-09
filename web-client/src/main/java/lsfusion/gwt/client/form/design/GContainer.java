package lsfusion.gwt.client.form.design;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReader;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.form.design.GContainerType.*;

public class GContainer extends GComponent {
    public String caption;

    public boolean main;

    public GContainerType type;
    public GFlexAlignment childrenAlignment;

    public int columns;

    public ArrayList<GComponent> children = new ArrayList<>();

    @Override
    public String toString() {
        return "GContainer" +
                "[" + sID + "]" +
                "[" + type + "]{" +
                "caption='" + caption + '\'' +
                ", alignment=" + getAlignment() +
                '}';
    }

    public GFlexAlignment getFlexJustify() {
        return childrenAlignment;
    }

    public boolean isTabbed() {
        return type == TABBED_PANE;
    }

    public int getFlexCount() {
        if(isTabbed())
            return 0;

        int count = 0;
        for(GComponent child : children)
            if(child.getFlex() > 0)
                count++;
        return count;
    }

    public boolean isSplit() {
        return type == HORIZONTAL_SPLIT_PANE || type == VERTICAL_SPLIT_PANE;
    }

    public boolean isScroll() {
        return type == SCROLL;
    }

    public List<GGrid> getAllGrids() {
        List<GGrid> grids = new ArrayList<>();
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
        List<GTreeGroup> grids = new ArrayList<>();
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
        List<GPropertyDraw> draws = new ArrayList<>();
        for (GComponent child : children) {
            if (child instanceof GPropertyDraw) {
                draws.add((GPropertyDraw) child);
            } else if (child instanceof GContainer) {
                draws.addAll(((GContainer) child).getAllPropertyDraws());
            }
        }
        return draws;
    }

    public GContainer findContainerByID(int id) {
        if (id == this.ID) return this;
        for (GComponent comp : children) {
            if (comp instanceof GContainer) {
                GContainer result = ((GContainer) comp).findContainerByID(id);
                if (result != null) return result;
            }
        }
        return null;
    }

    public boolean isSplitVertical() {
        return type == VERTICAL_SPLIT_PANE;
    }

    public boolean isSplitHorizontal() {
        return type == HORIZONTAL_SPLIT_PANE;
    }

    public boolean isHorizontal() {
        return isLinearHorizontal() || isSplitHorizontal();
    }

    public boolean isVertical() {
        return isLinearVertical() || isSplitVertical() || isColumns() || isTabbed();
    }

    public boolean isSingleElement() {
        return children.size() == 1;
    }
    public boolean isAlignCaptions() {
        if(!isVertical()) // later maybe it makes sense to support align captions for horizontal containers, but with no-wrap it doesn't make much sense
            return false;

        int notActions = 0;
        // only simple property draws
        for(GComponent child : children) {
            if(!(child instanceof GPropertyDraw) || ((GPropertyDraw) child).hasColumnGroupObjects() || (child.autoSize && ((GPropertyDraw) child).isAutoDynamicHeight()) || child.flex > 0 || ((GPropertyDraw) child).panelCaptionVertical)
                return false;

            if(!((GPropertyDraw)child).isAction())
                notActions++;
        }

        if(notActions <= 1)
            return false;

        return true;
    }

    public boolean isLastChild(GComponent child) {
        return children.indexOf(child) == children.size() - 1;
    }

    public boolean isLinearVertical() {
        return type == CONTAINERV;
    }

    public boolean isLinearHorizontal() {
        return type == CONTAINERH;
    }

    public boolean isLinear() {
        return isLinearVertical() || isLinearHorizontal();
    }

    public boolean isColumns() {
        return type == COLUMNS;
    }

    private class GCaptionReader implements GPropertyReader {

        public GCaptionReader() {
            sID = "_CONTAINER_" + "CAPTION" + "_" + GContainer.this.sID;
        }

        @Override
        public void update(GFormController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
            assert values.firstKey().isEmpty();
            Object value = values.firstValue();
            controller.setContainerCaption(GContainer.this, value != null ? value.toString() : null);
        }

        @Override
        public String getNativeSID() {
            return sID;
        }
    };
    public final GPropertyReader captionReader = new GCaptionReader();
}
