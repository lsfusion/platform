package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GBackgroundReader extends GExtraPropertyReader {

    public GBackgroundReader(){}

    public GBackgroundReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "BACKGROUND");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> keys) {
        controller.updateCellBackgroundValues(this, keys);
    }

    @Override
    public String getMetaField() { return "background"; }
    @Override
    public GMetaConverter getMetaConverter() { return GMetaConverter.COLOR; }
    @Override
    public String getColumnStatic(GComponent owner) { return ((GPropertyDraw) owner).getBackground(); } // the design colour, which a delivered value overrides
}
