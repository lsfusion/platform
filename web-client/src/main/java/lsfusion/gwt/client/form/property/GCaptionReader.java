package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GCaptionReader extends GExtraPropertyReader {
    public GCaptionReader(){}

    public GCaptionReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "CAPTION");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, Object> values) {
        controller.updatePropertyCaptions(this, values);
    }
}
