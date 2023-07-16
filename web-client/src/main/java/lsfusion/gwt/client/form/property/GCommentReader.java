package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GCommentReader extends GExtraPropertyReader {
    public GCommentReader(){}

    public GCommentReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "COMMENT");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updatePropertyComments(this, values);
    }
}
