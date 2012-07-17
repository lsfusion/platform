package platform.gwt.form2.client.form.classes;

import com.smartgwt.client.widgets.tree.TreeNode;
import platform.gwt.view2.classes.GObjectClass;

public class GClassTreeNode extends TreeNode {

    public final GObjectClass objectClass;

    public GClassTreeNode(GObjectClass objectClass) {
        this.objectClass = objectClass;

        setAttribute("name", objectClass.caption);
    }
}
