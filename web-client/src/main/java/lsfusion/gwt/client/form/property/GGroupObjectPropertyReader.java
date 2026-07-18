package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

// a presentation reader bound to a GROUP OBJECT (not a single property draw): delivered through the group's table
// controller, keyed by GGroupObjectValue. Most are per-row (background/foreground/select -> meta.row); customOptions is
// group-scoped (one value at EMPTY -> node.meta) -> see getMetaScope.
public abstract class GGroupObjectPropertyReader implements GPropertyReader {
    public int groupObjectID;

    public GGroupObjectPropertyReader() {
    }

    private String sID;

    public GGroupObjectPropertyReader(int groupObjectID, String prefix) {
        this.groupObjectID = groupObjectID;
        this.sID = "_ROW_" + prefix + "_" + groupObjectID;
    }

    @Override
    public String getNativeSID() {
        return sID;
    }

    // where this reader's presentation lands in data.meta: ROW = per-row (meta.row, keyed by the row, dirties the changed
    // rows), NODE = once for the whole group (node.meta, read at EMPTY, dirties only the node). Overridden by the group-scoped ones.
    public GMetaScope getMetaScope() { return GMetaScope.ROW; }

    protected abstract void update(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys);

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        update(controller.getGroupObjectController(controller.getGroupObject(groupObjectID)), values, updateKeys);
    }
}
