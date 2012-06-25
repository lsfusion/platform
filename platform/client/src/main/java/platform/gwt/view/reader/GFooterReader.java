package platform.gwt.view.reader;

import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GFooterReader implements GPropertyReader {
    public int readerID;
    public int groupobjectID;

    public GFooterReader(){}

    public GFooterReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupobjectID = groupObjectID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values) {
        controller.updateFooterValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupobjectID;
    }

}
