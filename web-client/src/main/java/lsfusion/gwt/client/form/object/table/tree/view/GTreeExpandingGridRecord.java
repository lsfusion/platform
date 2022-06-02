package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class GTreeExpandingGridRecord extends GTreeGridRecord {

    public GTreeExpandingGridRecord(GGroupObject group, GGroupObjectValue key, GTreeColumnValue treeValue) {
        super(group, key, treeValue);
    }

    public final static String expandingVirtualKey = "EXPANDING";
    @Override
    public String getVirtualKey() {
        return expandingVirtualKey;
    }
}
