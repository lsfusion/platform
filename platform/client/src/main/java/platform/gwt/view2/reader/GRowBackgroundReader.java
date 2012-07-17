package platform.gwt.view2.reader;

import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GRowBackgroundReader implements GPropertyReader {
    public int readerID;

    public GRowBackgroundReader(){}

    public GRowBackgroundReader(int readerID) {
        this.readerID = readerID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values) {
        controller.updateRowBackgroundValues(values);
    }

    @Override
    public int getGroupObjectID() {
        return readerID;
    }
}
