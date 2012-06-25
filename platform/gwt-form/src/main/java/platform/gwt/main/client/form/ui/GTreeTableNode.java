package platform.gwt.main.client.form.ui;

import com.smartgwt.client.widgets.tree.TreeNode;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.changes.GGroupObjectValue;

public class GTreeTableNode extends TreeNode {
    GGroupObject group;
    GGroupObjectValue key;

    public GTreeTableNode() {
        this(null, new GGroupObjectValue());
    }

    public GTreeTableNode(GGroupObject group, GGroupObjectValue key) {
        this.group = group;
        this.key = key;
    }
}
