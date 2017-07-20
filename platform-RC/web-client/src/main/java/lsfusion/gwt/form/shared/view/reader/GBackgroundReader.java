package lsfusion.gwt.form.shared.view.reader;

import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GBackgroundReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GBackgroundReader(){}

    public GBackgroundReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> keys, boolean updateKeys) {
        controller.updateBackgroundValues(this, keys);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
    }
}
