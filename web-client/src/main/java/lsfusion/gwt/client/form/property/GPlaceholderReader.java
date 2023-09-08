package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GPlaceholderReader extends GExtraPropertyReader {
    public GPlaceholderReader(){}

    public GPlaceholderReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "PLACEHOLDER");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updatePropertyPlaceholders(this, values);
    }
}
