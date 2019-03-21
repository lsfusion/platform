package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.Map;

public class GRowBackgroundReader implements GPropertyReader {
    public int readerID;

    public GRowBackgroundReader(){}

    public GRowBackgroundReader(int readerID) {
        this.readerID = readerID;
    }

    public void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateRowBackgroundValues(values);
    }

    @Override
    public int getGroupObjectID() {
        return readerID;
    }
}
