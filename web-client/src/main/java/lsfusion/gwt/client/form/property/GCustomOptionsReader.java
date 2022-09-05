package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

public class GCustomOptionsReader extends GRowPropertyReader {

    public GCustomOptionsReader(){}

    public GCustomOptionsReader(int readerID) {
        super(readerID, "CUSTOM_OPTIONS");
    }
    public void update(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateCustomOptionsValues(values);
    }
}
