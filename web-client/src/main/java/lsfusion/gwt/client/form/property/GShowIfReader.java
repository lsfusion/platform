package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GShowIfReader extends GExtraPropertyReader {

    public GShowIfReader(){}

    public GShowIfReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "SHOWIF");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateShowIfValues(this, values);
    }
}
