package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.Map;

public class GBackgroundReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GBackgroundReader(){}

    public GBackgroundReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GTableController controller, Map<GGroupObjectValue, Object> keys, boolean updateKeys) {
        controller.updateBackgroundValues(this, keys);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
    }
}
