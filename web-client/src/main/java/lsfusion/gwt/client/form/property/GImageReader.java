package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

public class GImageReader extends GExtraLabelReader {

    public GImageReader(){}

    public GImageReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "IMAGE");
    }

    public void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateImageValues(this, values);
    }

    @Override
    public String getAttributeField() { return "image"; }
    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.IMAGE; }
    @Override
    public boolean isColumnAttribute(GPropertyDraw draw) { return !draw.isAction(); } // server keys property images by column, action images by row
    @Override
    public String getStaticAttribute(GComponent owner) { return owner.getStaticImageHTML(); }
}
