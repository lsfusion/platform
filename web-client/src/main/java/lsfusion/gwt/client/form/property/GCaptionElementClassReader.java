package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GCaptionElementClassReader extends GExtraPropertyReader {

    public GCaptionElementClassReader() {
    }

    public GCaptionElementClassReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "CAPTIONELEMENTCLASS");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> keys) {
        controller.updateCellCaptionElementClasses(this, keys);
    }
}
