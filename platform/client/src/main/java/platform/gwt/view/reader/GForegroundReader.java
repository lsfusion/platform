package platform.gwt.view.reader;

import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GForegroundReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GForegroundReader(){}

    public GForegroundReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values) {
        controller.updateForegroundValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
}

}
