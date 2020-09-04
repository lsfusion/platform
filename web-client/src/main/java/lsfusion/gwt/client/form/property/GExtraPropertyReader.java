package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public abstract class GExtraPropertyReader implements GPropertyReader {

    public int propertyID;
    public int groupObjectID;

    public GExtraPropertyReader() {
    }

    private String sID; // optimization

    public GExtraPropertyReader(int propertyID, int groupObjectID, String prefix) {
        this.propertyID = propertyID;
        this.groupObjectID = groupObjectID;
        this.sID = "_PROPERTY_" + prefix + "_" + propertyID;
    }

    protected abstract void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, Object> values);

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        update(controller.getPropertyController(controller.getProperty(propertyID)), values);
    }

    @Override
    public String getSID() {
        return sID;
    }
}
