package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GValueTooltipReader extends GExtraPropertyReader {
    public GValueTooltipReader(){}

    public GValueTooltipReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "VALUETOOLTIP");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateValueTooltipValues(this, values);
    }
}
