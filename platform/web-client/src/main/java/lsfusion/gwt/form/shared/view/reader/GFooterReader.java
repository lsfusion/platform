package lsfusion.gwt.form.shared.view.reader;

import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;

import java.util.Map;

public class GFooterReader implements GPropertyReader {
    public int readerID;
    public int groupobjectID;

    public GFooterReader(){}

    public GFooterReader(int readerID, int groupObjectID) {
        this.readerID = readerID;
        this.groupobjectID = groupObjectID;
    }

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updateFooterValues(this, values);
    }

    @Override
    public int getGroupObjectID() {
        return groupobjectID;
    }

}
