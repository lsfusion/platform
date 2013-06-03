package lsfusion.client.descriptor.nodes.actions;

import javax.swing.tree.TreePath;

public interface DeletableTreeNode {
    public boolean deleteNode(TreePath selectionPath);
}
