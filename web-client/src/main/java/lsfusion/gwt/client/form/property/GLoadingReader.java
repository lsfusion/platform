package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GLoadingReader extends GExtraPropertyReader {

    public GLoadingReader(){}

    public GLoadingReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "LOADING");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, Object> values) {
        controller.updateLoadings(this, values);
    }

}
