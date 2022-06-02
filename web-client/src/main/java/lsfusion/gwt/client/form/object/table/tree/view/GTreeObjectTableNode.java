package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class GTreeObjectTableNode extends GTreeContainerTableNode implements GTreeChildTableNode {

    private GGroupObject group;
    private GGroupObjectValue key;

    private boolean expandable = true;

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

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }

    @Override
    public GTreeColumnValueType getColumnValueType() {
        return GTreeColumnValueType.get(isExpandable() ? (Boolean) hasExpandableChildren() : null);
    }
}
