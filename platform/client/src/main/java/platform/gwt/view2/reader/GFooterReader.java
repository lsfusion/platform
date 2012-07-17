package platform.gwt.view2.reader;

import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.logics.GGroupObjectLogicsSupplier;

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
