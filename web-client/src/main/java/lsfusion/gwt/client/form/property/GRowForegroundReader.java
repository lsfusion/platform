package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

public class GRowForegroundReader extends GRowPropertyReader {

    public GRowForegroundReader(){}

    public GRowForegroundReader(int readerID) {
        super(readerID, "FOREGROUND");
    }
    public void update(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateRowForegroundValues(values);
    }
}
