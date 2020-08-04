package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public class GImageReader extends GExtraPropertyReader {

    public GImageReader(){}

    public GImageReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "IMAGE");
    }

    public void update(GTableController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateImageValues(this, values);
    }
}
