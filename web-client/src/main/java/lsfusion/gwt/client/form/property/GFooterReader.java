package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public class GFooterReader extends GExtraPropertyReader {
    public GFooterReader(){}

    public GFooterReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "FOOTER");
    }

    public void update(GTableController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateFooterValues(this, values);
    }
}
