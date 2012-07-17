package platform.gwt.view2.reader;

import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GCaptionReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GCaptionReader(){}

    public GCaptionReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values) {
        controller.updateCaptionValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
    }

}
