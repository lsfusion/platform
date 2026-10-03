package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

// what comments a property: a label of it, as its caption is
public class GCommentReader extends GExtraLabelReader {

    public GCommentReader() {
    }

    public GCommentReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "COMMENT");
    }

    public void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updatePropertyComments(this, values);
    }

    @Override
    public String getAttributeField() { return "comment"; }
    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.TEXT; } // a trimmed string
    @Override
    public boolean isColumnAttribute(GPropertyDraw draw) { return true; } // read over the column groups (FormInstance)
    @Override
    public String getStaticAttribute(GComponent owner) { return ((GPropertyDraw) owner).comment; }
}
