package platform.gwt.form.shared.view.reader;

import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GRowForegroundReader implements GPropertyReader {
    public int readerID;

    public GRowForegroundReader(){}

    public GRowForegroundReader(int readerID) {
        this.readerID = readerID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateRowForegroundValues(values);
    }

    @Override
    public int getGroupObjectID() {
        return readerID;
    }
}
