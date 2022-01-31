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

public class GContainer extends GComponent {
    public String caption;
    public boolean collapsible;

    public boolean main;

    public boolean horizontal;
    public boolean tabbed;
    public GFlexAlignment childrenAlignment;

    public boolean grid;
    public boolean wrap;
    public Boolean alignCaptions;

    public int lines;
    public Integer lineSize;
    public boolean lineShrink;

    public ArrayList<GComponent> children = new ArrayList<>();

    @Override
    public String toString() {
        return "GContainer" +
                "[" + sID + "] " +
                getContainerType() + ", " +
                "caption='" + caption + "', " +
                "alignment=" + getAlignment() +
                '}';
    }

    public void removeFromChildren(GComponent component) {
        component.container = null;
        children.remove(component);
    }

    public void add(GComponent component) {
        if (component.container != null) {
            component.container.removeFromChildren(component);
        }
        children.add(component);
        component.container = this;
    }

    public GFlexAlignment getFlexAlignment() {
        return childrenAlignment;
    }

    public int getFlexCount() {
        if(tabbed)
            return 0;

        int count = 0;
        for(GComponent child : children)
            if(child.getFlex() > 0)
                count++;
        return count;
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

    public String getContainerType() {
        return "horizontal=" + horizontal + ", tabbed=" + tabbed;
    }

    public boolean isSingleElement() {
        return children.size() == 1;
    }

    public boolean isVertical() {
        // in wrapped grid it makes sense to "reverse" the direction (it is more obvious)
        return horizontal == (grid && isWrap());
    }

    public boolean isWrap() {
        // we cannot wrap grid with aligned captions (since there is no way to stick caption and value together)
        if(grid && isAlignCaptions())
            return false;

        // grid auto-fit (used for wrap) doesn't support min-content / auto / ...
        if(grid && lineSize == null)
            return false;

        return wrap;
    }

    public boolean isGrid() {
        return grid;
    }
    public boolean isAlignCaptions() {
        // align caption has a higher priority than wrap
        if(horizontal) // later maybe it makes sense to support align captions for horizontal containers, but with no-wrap it doesn't make much sense
            return false;
        if(children.size() <= lines) // if there are fewer components than lines, there is no point in creating grids (however later it makes sense to avoid creating grids for specific lines)
            return false;

        if (alignCaptions != null) {
            return alignCaptions;
        }

        boolean otherAligned = false;
        // only simple property draws
        for(GComponent child : children) {
            if(child.isAlignCaption()) {
                if(otherAligned)
                    return true;
                else
                    otherAligned = true;
            }
        }

        return false;
    }
    
    public Integer getLineSize() {
        return lineSize;
    }

    public boolean isLineShrink() {
        return lineShrink;
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
    }
    public final GPropertyReader captionReader = new GCaptionReader();
}
