package platform.gwt.form2.client.form.ui;

import platform.gwt.view2.GGroupObject;
import platform.gwt.view2.changes.GGroupObjectValue;

public class GTreeTableNode {//extends TreeNode {
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
