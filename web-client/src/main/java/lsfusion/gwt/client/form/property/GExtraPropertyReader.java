package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

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

    // where the platform draws the property: its controller's own update for this reader
    public abstract void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values);

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        update(controller.getPropertyController(controller.getProperty(propertyID)), values, partial);
    }
    // what kind of reader it is: one React draws nothing from - loading, SHOWIF, classes, change keys
    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        controller.updateOther(this, values, partial);
    }

    @Override
    public String getNativeSID() {
        return sID;
    }
}
