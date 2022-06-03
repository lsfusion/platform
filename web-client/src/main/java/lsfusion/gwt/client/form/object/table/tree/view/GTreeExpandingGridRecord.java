package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class GTreeExpandingGridRecord extends GTreeGridRecord {

    public GTreeExpandingGridRecord(int rowIndex, GTreeContainerTableNode node, GTreeColumnValue treeValue) {
        super(rowIndex, node, treeValue);
    }

    public final static String expandingVirtualKey = "EXPANDING";
    @Override
    public String getVirtualKey() {
        return expandingVirtualKey;
    }
}
