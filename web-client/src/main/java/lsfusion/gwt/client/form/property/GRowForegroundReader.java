package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPlatformTableController;

public class GRowForegroundReader extends GGroupObjectPropertyReader {

    public GRowForegroundReader(){}

    public GRowForegroundReader(int readerID) {
        super(readerID, "FOREGROUND");
    }
    public void updatePlatform(GPlatformTableController<?, ?, ?> controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        controller.updateRowForegroundValues(values);
    }

    @Override
    public String getAttributeField() { return "foreground"; }
    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.COLOR; }
}
