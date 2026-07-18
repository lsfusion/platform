package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GReadOnlyReader extends GExtraPropertyReader {

    public GReadOnlyReader(){}

    public GReadOnlyReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "READONLY");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateReadOnlyValues(this, values);
    }

    @Override
    public String getMetaField() { return "readOnly"; }
    @Override
    public String getMetaField(PValue value) { return getEditabilityField(PValue.get3SBooleanValue(value)); }
    @Override
    public GMetaConverter getMetaConverter() { return GMetaConverter.FLAG; }

    private static native String getEditabilityField(Object value) /*-{
        return value === true ? "disabled" : value === false ? "readOnly" : null;
    }-*/;
}
