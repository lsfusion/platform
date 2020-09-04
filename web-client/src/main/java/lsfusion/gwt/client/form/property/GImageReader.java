package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GImageReader extends GExtraPropertyReader {

    public GImageReader(){}

    public GImageReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "IMAGE");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, Object> values) {
        controller.updateImageValues(this, values);
    }
}
