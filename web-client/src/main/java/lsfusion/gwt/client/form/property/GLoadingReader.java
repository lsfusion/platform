package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

public class GLoadingReader extends GExtraPropertyReader {

    public GLoadingReader(){}

    public GLoadingReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "LOADING");
    }

    public void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateLoadings(this, values);
    }

}
