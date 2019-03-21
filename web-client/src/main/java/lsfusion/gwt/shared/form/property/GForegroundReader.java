package lsfusion.gwt.shared.form.property;

import lsfusion.gwt.shared.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.Map;

public class GForegroundReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GForegroundReader(){}

    public GForegroundReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateForegroundValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
}

}
