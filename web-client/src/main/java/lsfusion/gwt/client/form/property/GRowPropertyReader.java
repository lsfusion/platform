package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

public abstract class GRowPropertyReader implements GPropertyReader {
    public int groupObjectID;

    public GRowPropertyReader() {
    }

    private String sID;

    public GRowPropertyReader(int groupObjectID, String prefix) {
        this.groupObjectID = groupObjectID;
        this.sID = "_ROW_" + prefix + "_" + groupObjectID;
    }

    @Override
    public String getSID() {
        return sID;
    }

    protected abstract void update(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys);

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        update(controller.getGroupObjectController(controller.getGroupObject(groupObjectID)), values, updateKeys);
    }
}
