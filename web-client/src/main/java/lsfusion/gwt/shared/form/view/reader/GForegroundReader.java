package lsfusion.gwt.shared.form.view.reader;

import lsfusion.gwt.shared.form.view.changes.GGroupObjectValue;
import lsfusion.gwt.shared.form.view.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GForegroundReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GForegroundReader(){}

    public GForegroundReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateForegroundValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
}

}
