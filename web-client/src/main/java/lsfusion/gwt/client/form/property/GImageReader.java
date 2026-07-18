package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GImageReader extends GExtraPropertyReader {

    public GImageReader(){}

    public GImageReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "IMAGE");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateImageValues(this, values);
    }

    @Override
    public String getMetaField() { return "image"; }
    @Override
    public GMetaConverter getMetaConverter() { return GMetaConverter.IMAGE; }
    @Override
    public boolean isColumnLevel(GPropertyDraw draw) { return !draw.isAction(); } // server keys property images by column, action images by row
    @Override
    public String getColumnStatic(GComponent owner) { return owner.getStaticImageHTML(); }
}
