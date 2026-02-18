package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

public class GRowSelectReader extends GRowPropertyReader {

    public GRowSelectReader(){}

    public GRowSelectReader(int readerID) {
        super(readerID, "SELECT");
    }

    public void update(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        controller.updateRowSelectValues(values, updateKeys);
    }
}
