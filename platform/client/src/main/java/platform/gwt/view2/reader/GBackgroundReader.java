package platform.gwt.view2.reader;

import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GBackgroundReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GBackgroundReader(){}

    public GBackgroundReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> keys) {
        controller.updateBackgroundValues(this, keys);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
    }
}
