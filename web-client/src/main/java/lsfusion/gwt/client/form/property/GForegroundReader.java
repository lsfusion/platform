package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

public class GForegroundReader extends GExtraAttributeReader {

    public GForegroundReader(){}

    public GForegroundReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "FOREGROUND");
    }

    public void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateCellForegroundValues(this, values);
    }

    @Override
    public String getAttributeField() { return "foreground"; }
    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.COLOR; }
    @Override
    public String getStaticAttribute(GComponent owner) { return ((GPropertyDraw) owner).getForeground(); } // the design colour, which a delivered value overrides
}
