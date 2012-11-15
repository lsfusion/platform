package platform.client.descriptor.nodes.actions;

import javax.swing.tree.TreePath;

public interface AddableTreeNode {
    public Object[] addNewElement(TreePath selectionPath);
}
