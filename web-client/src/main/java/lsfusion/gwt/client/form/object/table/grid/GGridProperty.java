package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.PValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// a TABLE - the grid of a list's group not in a tree (GGrid), or a tree, one for all its groups (GTreeGroup) -: what
// draws the rows, and what goes over them: its toolbar, its FILTERS box with the filters it holds, and its filter
// controls (as the server's GridPropertyView has them)
public abstract class GGridProperty extends GComponent {

    public GToolbar toolbar;
    public GContainer filtersContainer;
    public ArrayList<GFilter> filters = new ArrayList<>();
    public GFilterControls filterControls;

    // ... and the groups whose rows it draws: the grid's group, or the tree's groups, top down
    public abstract List<GGroupObject> getGroups();
    // ... and its kind, told to whichever of the two is given for it: a grid, or a tree
    public abstract <R> R match(Function<GGrid, R> grid, Function<GTreeGroup, R> tree);

    public int captionHeight;
    public int captionCharHeight;

    public int lineWidth;
    public int lineHeight;

    public Boolean boxed;
    public Boolean resizeOverflow;

    public GSize getCaptionHeight(boolean needNotNull) {
        return GPropertyDraw.getHeight(captionHeight, captionCharHeight, font, needNotNull);
    }

    protected GGroupObject getLastGroup() {
        List<GGroupObject> groups = getGroups();
        return groups.get(groups.size() - 1);
    }
    protected abstract GSize getExtraWidth();

    @Override
    protected GSize getDefaultWidth() {
        return getDefaultSize().first;
    }

    @Override
    protected GSize getDefaultHeight() {
        return getDefaultSize().second;
    }

    protected Pair<GSize, GSize> getDefaultSize() {
        return getLastGroup().getSize(lineHeight, lineWidth, getExtraWidth(), getCaptionHeight(true));
    }

    public String valueClass;

    private class GValueElementClassReader extends GComponentReader {
        public GValueElementClassReader() {
            super(GGridProperty.this, "_GRID_VALUEELEMENTCLASSREADER");
        }

        @Override
        public void updatePlatform(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
            controller.getFormLayout().setValueClass(GGridProperty.this, PValue.getClassStringValue(values.get(GGroupObjectValue.EMPTY)));
        }
    }
    public final GPropertyReader valueElementClassReader = new GValueElementClassReader();
}
