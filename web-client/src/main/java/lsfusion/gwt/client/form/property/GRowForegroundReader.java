package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.Map;

public class GRowForegroundReader implements GPropertyReader {
    public int readerID;

    public GRowForegroundReader(){}

    public GRowForegroundReader(int readerID) {
        this.readerID = readerID;
    }

    public void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateRowForegroundValues(values);
    }

    @Override
    public int getGroupObjectID() {
        return readerID;
    }
}
