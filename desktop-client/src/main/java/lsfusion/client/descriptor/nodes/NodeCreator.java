package lsfusion.client.descriptor.nodes;

import lsfusion.client.tree.ClientTreeNode;

public interface NodeCreator {
    public ClientTreeNode createNode(Object context);
}
