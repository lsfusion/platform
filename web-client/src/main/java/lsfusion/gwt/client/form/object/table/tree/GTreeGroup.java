package lsfusion.gwt.client.form.object.table.tree;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.PValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GTreeGroup extends GGridProperty {
    // its name in the form - TREE t's t -, which a react view's node of the tree is named by (GReactFormData.TreeNode)
    public String name;
    public ArrayList<GGroupObject> groups = new ArrayList<>();

    public int hierarchicalWidth;
    public String hierarchicalCaption;

    public GSize getExpandWidth() {
        if(hierarchicalWidth > 0) {
            return GSize.CONST(hierarchicalWidth);
        }

        GSize size = GSize.ZERO;
        for (GGroupObject groupObject : groups) {
            GSize groupSize = GSize.CONST(20);
            if(groupObject.isRecursive)
                groupSize = groupSize.scale(4);
            size = size.add(groupSize);
        }
        return size;
    }

    @Override
    public List<GGroupObject> getGroups() {
        return groups;
    }
    @Override
    public <R> R match(Function<GGrid, R> grid, Function<GTreeGroup, R> tree) {
        return tree.apply(this);
    }

    @Override
    protected GSize getExtraWidth() {
        return getExpandWidth();
    }

    public GGroupObjectValue filterRowKeys(GGroupObject groupObject, GGroupObjectValue fullCurrentKey) {
        return fullCurrentKey.filter(groups.subList(0, groups.indexOf(groupObject) + 1));
    }

    private class GHierarchicalCaptionReader extends GComponentReader {
        public GHierarchicalCaptionReader() {
            super(GTreeGroup.this, "_TREE_HIERARCHICALCAPTIONREADER");
        }

        @Override
        public void updatePlatform(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
            controller.getFormLayout().setHierarchicalCaption(GTreeGroup.this, PValue.getStringValue(values.get(GGroupObjectValue.EMPTY)));
        }
    }
    public final GPropertyReader hierarchicalCaptionReader = new GTreeGroup.GHierarchicalCaptionReader();
}
