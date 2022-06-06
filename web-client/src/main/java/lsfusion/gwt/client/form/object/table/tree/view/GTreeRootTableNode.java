package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class GTreeRootTableNode extends GTreeContainerTableNode {

    public GGroupObject getGroup() {
        return null;
    }

    public GGroupObjectValue getKey() {
        return GGroupObjectValue.EMPTY;
    }

    public boolean isExpandable() {
        return true;
    }
}
