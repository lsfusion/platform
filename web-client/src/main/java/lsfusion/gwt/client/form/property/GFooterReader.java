package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

public class GFooterReader extends GExtraLabelReader {
    public GFooterReader(){}

    public GFooterReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "FOOTER");
    }

    public void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateFooterValues(this, values);
    }

    @Override
    public String getAttributeField() { return "footer"; }
    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.FOOTER; }
    @Override
    public boolean isColumnAttribute(GPropertyDraw draw) { return true; } // the column footer (no static design fallback)
}
