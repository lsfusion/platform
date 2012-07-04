package platform.gwt.form.client.form.classes;

import com.smartgwt.client.widgets.tree.TreeNode;
import platform.gwt.view.classes.GObjectClass;

public class GClassTreeNode extends TreeNode {

    public final GObjectClass objectClass;

    public GClassTreeNode(GObjectClass objectClass) {
        this.objectClass = objectClass;

        setAttribute("name", objectClass.caption);
    }
}
