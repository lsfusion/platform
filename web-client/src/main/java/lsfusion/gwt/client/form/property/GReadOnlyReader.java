package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.Map;

public class GReadOnlyReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GReadOnlyReader(){}

    public GReadOnlyReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateReadOnlyValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
    }
}
