package platform.gwt.view2.reader;

import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GRowForegroundReader implements GPropertyReader {
    public int readerID;

    public GRowForegroundReader(){}

    public GRowForegroundReader(int readerID) {
        this.readerID = readerID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values) {
        controller.updateRowForegroundValues(values);
    }

    @Override
    public int getGroupObjectID() {
        return readerID;
    }
}
