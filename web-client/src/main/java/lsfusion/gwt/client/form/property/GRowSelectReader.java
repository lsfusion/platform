package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

public class GRowSelectReader extends GGroupObjectPropertyReader {

    public GRowSelectReader(){}

    public GRowSelectReader(int readerID) {
        super(readerID, "SELECT");
    }

    public void update(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        controller.updateRowSelectValues(values, updateKeys);
    }

    @Override
    public String getMetaField() { return "selected"; }
    @Override
    public GMetaConverter getMetaConverter() { return GMetaConverter.FLAG; } // present -> true (2-state), NOT get3SBooleanValue
}
