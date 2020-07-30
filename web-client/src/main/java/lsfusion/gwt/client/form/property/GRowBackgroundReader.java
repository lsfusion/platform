package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public class GRowBackgroundReader extends GRowPropertyReader {

    public GRowBackgroundReader(){}

    public GRowBackgroundReader(int readerID) {
        super(readerID, "BACKGROUND");
    }

    public void update(GTableController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateRowBackgroundValues(values);
    }
}
