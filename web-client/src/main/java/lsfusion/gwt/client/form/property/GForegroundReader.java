package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public class GForegroundReader extends GExtraPropertyReader {

    public GForegroundReader(){}

    public GForegroundReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "FOREGROUND");
    }

    public void update(GTableController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateCellForegroundValues(this, values);
    }
}
