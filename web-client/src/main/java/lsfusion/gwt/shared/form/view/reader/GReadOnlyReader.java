package lsfusion.gwt.shared.form.view.reader;

import lsfusion.gwt.shared.changes.GGroupObjectValue;
import lsfusion.gwt.shared.form.view.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GReadOnlyReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GReadOnlyReader(){}

    public GReadOnlyReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateReadOnlyValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
    }
}
