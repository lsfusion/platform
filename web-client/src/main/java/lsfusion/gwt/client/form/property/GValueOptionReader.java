package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GValueOptionReader extends GExtraPropertyReader {
    public GValueOptionReader(){}

    String type;
    public GValueOptionReader(int readerID, int groupObjectID, String type) {
        super(readerID, groupObjectID, type);
        this.type = type;
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        switch (type) {
            case "VALUETOOLTIP":
                controller.updateValueTooltipValues(this, values);
                break;
            case "VALUEELEMENTCLASS":
                controller.updateCellValueElementClasses(this, values);
                break;
            case "VALUEATTR":
                controller.updateValueAttrValues(this, values);
                break;
        }
    }
}
