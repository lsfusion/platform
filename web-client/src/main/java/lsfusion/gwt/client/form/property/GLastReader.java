package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPlatformPropertyController;

public class GLastReader extends GExtraPropertyReader {

    public int index;
    public GType type; // of the order, not of the property

    public GLastReader() {
    }

    public GLastReader(int readerID, int index, int groupObjectID, GType type) {
        super(readerID, groupObjectID, "LAST");
        this.index = index;
        this.type = type;
    }

    @Override
    public void updatePlatform(GPlatformPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateLastValues(this, values);
    }
}
