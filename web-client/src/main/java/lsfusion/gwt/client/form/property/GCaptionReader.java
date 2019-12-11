package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.Map;

public class GCaptionReader implements GPropertyReader {
    public int readerID;
    public int groupObjectID;

    public GCaptionReader(){}

    public GCaptionReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupObjectID = groupObjectID;
    }

    public void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updatePropertyCaptions(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupObjectID;
    }

}
