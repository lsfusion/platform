package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.object.GGroupObject;

import lsfusion.gwt.client.GForm;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

// a presentation reader bound to a GROUP OBJECT (not a single property draw): delivered through the group's table
// controller, keyed by GGroupObjectValue. Most are per-row (background/foreground/select -> direct row fields); options is
// group-scoped (one value at EMPTY -> direct on the group) -> see getAttributeScope.
public abstract class GGroupObjectPropertyReader implements GPropertyReader {
    public int groupObjectID;

    @Override
    public GGroupObject getAttributeGroup(GForm form) { return form.getGroupObject(groupObjectID); } // a group-object reader carries its ID

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


    protected abstract void update(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys);

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        update(controller.getGroupObjectController(controller.getGroupObject(groupObjectID)), values, updateKeys);
    }
}
