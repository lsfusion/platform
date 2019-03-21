package lsfusion.client.form.object.table.tree;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;

import java.util.List;
import java.util.Map;

public class TreeGroupNode extends DefaultMutableTreeTableNode {
    public final ClientGroupObject group;
    public final ClientGroupObjectValue key;
    private boolean expandable;

    private final GroupTreeTableModel model;

    public TreeGroupNode(GroupTreeTableModel model) {
        this(model, null, ClientGroupObjectValue.EMPTY);
    }

    public TreeGroupNode(GroupTreeTableModel model, ClientGroupObject group, ClientGroupObjectValue key) {
        this.model = model;
        this.group = group;
        this.key = key;
    }

    public List<MutableTreeTableNode> getChildren() {
        return children;
    }

    public void removeAllChildren() {
        while (getChildCount() > 0) {
            removeFirstChild();
        }
    }

    public void removeFirstChild() {
        removeChild((MutableTreeTableNode) getChildAt(0));
    }

    public void removeChild(MutableTreeTableNode child) {
        model.removeNodeFromParent(child);
    }

    public void addChild(MutableTreeTableNode child) {
        model.insertNodeInto(child, this, getChildCount());
    }

    @Override
    public String toString() {
        for (ClientPropertyDraw property : model.properties) {
            Map<ClientGroupObjectValue, Object> propValues = model.values.get(property);
            if (propValues != null && propValues.containsKey(key)) {
                return BaseUtils.toCaption(propValues.get(key));
            }
        }

        return "";
    }

    public boolean hasOnlyExpandningNodeAsChild() {
        return getChildCount() == 1 && getChildAt(0) instanceof ExpandingTreeTableNode;
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }
}
