package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

public class GValueElementClassReader extends GExtraPropertyReader {

    public GValueElementClassReader() {
    }

    public GValueElementClassReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "VALUEELEMENTCLASS");
    }

    public void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> keys) {
        controller.updateCellValueElementClasses(this, keys);
    }

    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.CLASS; }
}