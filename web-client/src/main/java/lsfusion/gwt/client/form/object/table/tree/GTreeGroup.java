package lsfusion.gwt.client.form.object.table.tree;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.PValue;

import java.util.ArrayList;

public class GTreeGroup extends GGridProperty {
    public ArrayList<GGroupObject> groups = new ArrayList<>();

    public GContainer filtersContainer;
    public GFilterControls filterControls;
    public ArrayList<GFilter> filters = new ArrayList<>();

    public Boolean boxed;

    public GToolbar toolbar;
    
    public boolean expandOnClick;
    public int hierarchicalWidth;
    public String hierarchicalCaption;

    public Boolean resizeOverflow;

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

    protected GGroupObject getLastGroup() {
        return groups.get(groups.size() - 1);
    }

    @Override
    protected GSize getExtraWidth() {
        return getExpandWidth();
    }

    public GGroupObjectValue filterRowKeys(GGroupObject groupObject, GGroupObjectValue fullCurrentKey) {
        return fullCurrentKey.filter(groups.subList(0, groups.indexOf(groupObject) + 1));
    }

    private class GHierarchicalCaptionReader implements GPropertyReader {
        private String sID;

        public GHierarchicalCaptionReader() {
        }

        @Override
        public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
            controller.getFormLayout().setHierarchicalCaption(GTreeGroup.this, PValue.getStringValue(values.get(GGroupObjectValue.EMPTY)));
        }

        @Override
        public String getNativeSID() {
            if(sID == null) {
                sID = "_TREE_" + "HIERARCHICALCAPTIONREADER" + "_" + GTreeGroup.this.sID;
            }
            return sID;
        }
    }
    public final GPropertyReader hierarchicalCaptionReader = new GTreeGroup.GHierarchicalCaptionReader();
}
