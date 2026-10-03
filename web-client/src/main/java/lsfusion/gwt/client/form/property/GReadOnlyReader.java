package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

public class GReadOnlyReader extends GExtraAttributeReader {

    public GReadOnlyReader(){}

    public GReadOnlyReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "READONLY");
    }

    public void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateReadOnlyValues(this, values);
    }

    @Override
    public String getAttributeField() { return "readOnly"; }
    @Override
    public String getAttributeField(PValue value) { return getEditabilityField(PValue.get3SBooleanValue(value)); }
    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.FLAG; }

    private static native String getEditabilityField(Object value) /*-{
        return value === true ? "disabled" : value === false ? "readOnly" : null;
    }-*/;
}
