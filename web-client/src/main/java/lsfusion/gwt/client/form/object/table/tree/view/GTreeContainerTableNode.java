package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.ArrayList;

public abstract class GTreeContainerTableNode implements GTreeTableNode {

    private ArrayList<GTreeChildTableNode> children;

    public Boolean pendingExpanding; // true - expanding, false - collapsing
    public long pendingExpandingRequestIndex;

    public GTreeContainerTableNode() {
        this.children = new ArrayList<>();
    }

    public abstract GGroupObject getGroup();

    public abstract GGroupObjectValue getKey();

    public ArrayList<GTreeChildTableNode> getChildren() {
        return children;
    }

    public void addNode(int index, GTreeChildTableNode child) {
        children.add(index, child);
    }

    public void setChildren(ArrayList<GTreeChildTableNode> newChildren) {
        children = newChildren;
    }

    public boolean isLast(GTreeChildTableNode child) {
        return children.get(children.size() - 1).equals(child);
    }

    public void removeNode(int index) {
        children.remove(index);
    }

    public void removeNodes() {
        children.clear();
    }

    public boolean hasExpandableChildren() {
        assert isExpandable();
        return !children.isEmpty();
    }

    public abstract boolean isExpandable();

    public void setPendingExpanding(Boolean open, long requestIndex) {
        pendingExpanding = open;
        pendingExpandingRequestIndex = requestIndex;
    }

    public boolean hasOnlyExpandingTreeTableNodes() {
        return children.size() == 1 && children.get(0) instanceof GTreeExpandingTableNode;
    }
}
