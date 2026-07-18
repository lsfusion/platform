package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GCaptionReader extends GExtraPropertyReader {
    public GCaptionReader(){}

    public GCaptionReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "CAPTION");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updatePropertyCaptions(this, values);
    }

    @Override
    public String getMetaField() { return "caption"; }
    @Override
    public GMetaConverter getMetaConverter() { return GMetaConverter.CAPTION; }
    @Override
    public boolean isColumnLevel(GPropertyDraw draw) { return true; } // the column header
    @Override
    public String getColumnStatic(GComponent owner) { return owner.getStaticCaption(); }
}
