package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GForegroundReader extends GExtraPropertyReader {

    public GForegroundReader(){}

    public GForegroundReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "FOREGROUND");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateCellForegroundValues(this, values);
    }

    @Override
    public String getMetaField() { return "foreground"; }
    @Override
    public GMetaConverter getMetaConverter() { return GMetaConverter.COLOR; }
    @Override
    public String getColumnStatic(GComponent owner) { return ((GPropertyDraw) owner).getForeground(); } // the design colour, which a delivered value overrides
}
