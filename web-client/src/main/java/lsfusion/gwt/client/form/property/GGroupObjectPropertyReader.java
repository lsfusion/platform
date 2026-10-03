package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

// a presentation reader bound to a GROUP OBJECT (not a single property draw): delivered through the group's table
// controller, keyed by GGroupObjectValue. Most are per-row (background/foreground/select -> direct row fields); options is
// group-scoped (one value at EMPTY -> direct on the group) -> see getAttributeScope.
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


    // where the platform draws the group's rows: its grid's or tree's own update for this reader
    public abstract void updateLsf(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys);

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        controller.getGroupController(controller.getGroupObject(groupObjectID)).updateAttribute(this, values, partial);
    }
    // ... an attribute of its rows or of the group, as every one of them is: it names the field it is projected into
    @Override
    public abstract String getAttributeField();
}
