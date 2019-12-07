package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.Map;

public class GLastReader implements GPropertyReader {

    public int propertyID;
    public int index;
    public int groupObjectID;

    public GLastReader() {
    }

    public GLastReader(int propertyID, int index, int groupObjectID) {
        this.propertyID = propertyID;
        this.index = index;
        this.groupObjectID = groupObjectID;
    }

    @Override
    public void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateLastValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
    }
}
