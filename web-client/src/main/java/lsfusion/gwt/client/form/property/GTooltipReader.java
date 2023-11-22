package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GTooltipReader extends GExtraPropertyReader {
    public GTooltipReader(){}

    public GTooltipReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "TOOLTIP");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateTooltipValues(this, values);
    }
}
