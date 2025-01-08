package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GParentElementClassReader extends GExtraPropertyReader {

    public GParentElementClassReader() {
    }

    public GParentElementClassReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "PARENTELEMENTCLASS");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> keys) {
        controller.updateCellParentElementClasses(this, keys);
    }
}