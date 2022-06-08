package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class GTreeObjectTableNode extends GTreeContainerTableNode implements GTreeChildTableNode {

    private GGroupObject group;
    private GGroupObjectValue key;

    public GTreeObjectTableNode(GGroupObject group, GGroupObjectValue key) {
        this.group = group;
        this.key = key;
    }

    public GGroupObject getGroup() {
        return group;
    }

    public GGroupObjectValue getKey() {
        return key;
    }

    @Override
    public GTreeColumnValueType getColumnValueType() {
        return GTreeColumnValueType.get(isExpandable() ? (Boolean) hasExpandableChildren() : null);
    }
}
