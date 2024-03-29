package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GLastReader extends GExtraPropertyReader {

    public int index;

    public GLastReader() {
    }

    public GLastReader(int readerID, int index, int groupObjectID) {
        super(readerID, groupObjectID, "LAST");
        this.index = index;
    }

    @Override
    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateLastValues(this, values);
    }
}
