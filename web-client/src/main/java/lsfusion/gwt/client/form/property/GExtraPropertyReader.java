package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.design.GComponent;

import lsfusion.gwt.client.GForm;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public abstract class GExtraPropertyReader implements GPropertyReader {

    public int propertyID;

    @Override
    public GComponent getAttributeComponent(GForm form) { return form.getProperty(propertyID); } // a property's reader carries its ID
    public int groupObjectID;

    public GExtraPropertyReader() {
    }

    private String sID; // optimization

    public GExtraPropertyReader(int propertyID, int groupObjectID, String prefix) {
        this.propertyID = propertyID;
        this.groupObjectID = groupObjectID;
        this.sID = "_PROPERTY_" + prefix + "_" + propertyID;
    }

    protected abstract void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values);

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        update(controller.getPropertyController(controller.getProperty(propertyID)), values);
    }

    @Override
    public String getNativeSID() {
        return sID;
    }
}
