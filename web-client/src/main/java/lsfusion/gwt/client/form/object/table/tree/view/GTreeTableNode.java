package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.ArrayList;

public class GTreeTableNode {
    private GGroupObject group;
    private GGroupObjectValue key;
    private GTreeTableNode parent;
    private ArrayList<GTreeTableNode> children;
    private boolean open = false;
    private boolean expandable = true;

    public GTreeTableNode() {
        this(null, GGroupObjectValue.EMPTY);
    }

    public GTreeTableNode(GGroupObject group, GGroupObjectValue key) {
        this.group = group;
        this.key = key;
        children = new ArrayList<>();
    }

    public GGroupObject getGroup() {
        return group;
    }

    public GGroupObjectValue getKey() {
        return key;
    }

    public GTreeTableNode getParent() {
        return parent;
    }

    public ArrayList<GTreeTableNode> getChildren() {
        return children;
    }

    public GTreeTableNode getChild(int index) {
        return children.get(index);
    }

    public void setParent(GTreeTableNode parent) {
        this.parent = parent;
    }

    public void addNode(int index, GTreeTableNode child) {
        children.add(index, child);
        child.setParent(this);
    }

    public void setChildren(ArrayList<GTreeTableNode> newChildren) {
        children = newChildren;
        for(GTreeTableNode child : newChildren)
            child.setParent(this);
    }

    public boolean isLast(GTreeTableNode child) {
        return children.get(children.size() - 1).equals(child);
    }

    public void removeNode(int index) {
        children.remove(index);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        if (expandable) {
            this.open = open;
        }
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
        if (!expandable) {
            open = false;
        }
    }
}
