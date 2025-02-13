package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GGridElementClassReader extends GExtraPropertyReader {

    public GGridElementClassReader() {
    }

    public GGridElementClassReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "GRIDELEMENTCLASS");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> keys) {
        controller.updateCellGridElementClasses(this, keys);
    }
}