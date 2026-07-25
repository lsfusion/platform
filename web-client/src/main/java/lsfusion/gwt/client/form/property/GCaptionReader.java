package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GCaptionReader extends GExtraPropertyReader {

    @Override
    public boolean isDescriptorAttribute() { return true; } // the caption/image an lsf child hands to React
    public GCaptionReader(){}

    public GCaptionReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "CAPTION");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updatePropertyCaptions(this, values);
    }

    @Override
    public String getAttributeField() { return "caption"; }
    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.CAPTION; }
    @Override
    public boolean isColumnAttribute(GPropertyDraw draw) { return true; } // the column header
    @Override
    public String getStaticAttribute(GComponent owner) { return owner.getStaticCaption(); }
}
