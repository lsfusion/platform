package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public class GReadOnlyReader extends GExtraPropertyReader {

    public GReadOnlyReader(){}

    public GReadOnlyReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "READONLY");
    }

    public void update(GTableController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateReadOnlyValues(this, values);
    }
}
