package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GCommentElementClassReader extends GExtraPropertyReader {

    public GCommentElementClassReader() {
    }

    public GCommentElementClassReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "COMMENTELEMENTCLASS");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> keys) {
        controller.updateCellCommentElementClasses(this, keys);
    }
}
