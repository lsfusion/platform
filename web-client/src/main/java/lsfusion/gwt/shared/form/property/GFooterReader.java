package lsfusion.gwt.shared.form.property;

import lsfusion.gwt.shared.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.Map;

public class GFooterReader implements GPropertyReader {
    public int readerID;
    public int groupobjectID;

    public GFooterReader(){}

    public GFooterReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupobjectID = groupObjectID;
    }

    public void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateFooterValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupobjectID;
    }

}
