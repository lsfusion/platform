package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GValueAttrReader extends GExtraPropertyReader {
    public GValueAttrReader(){}

    public GValueAttrReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "VALUEATTR");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateValueAttrValues(this, values);
    }
}
