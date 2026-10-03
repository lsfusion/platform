package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

public class GShowIfReader extends GExtraPropertyReader {

    public GShowIfReader(){}

    public GShowIfReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "SHOWIF");
    }

    public void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateShowIfValues(this, values);
    }
}
