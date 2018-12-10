package lsfusion.gwt.shared.form.view.reader;

import lsfusion.gwt.shared.form.view.changes.GGroupObjectValue;
import lsfusion.gwt.shared.form.view.logics.GGroupObjectLogicsSupplier;

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
