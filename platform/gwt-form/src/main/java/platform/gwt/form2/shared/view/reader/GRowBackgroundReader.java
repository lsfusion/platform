package platform.gwt.form2.shared.view.reader;

import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GRowBackgroundReader implements GPropertyReader {
    public int readerID;

    public GRowBackgroundReader(){}

    public GRowBackgroundReader(int readerID) {
        this.readerID = readerID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateRowBackgroundValues(values);
    }

    @Override
    public int getGroupObjectID() {
        return readerID;
    }
}
