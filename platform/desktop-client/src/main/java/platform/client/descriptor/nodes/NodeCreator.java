package platform.client.descriptor.nodes;

import platform.client.tree.ClientTreeNode;

public interface NodeCreator {
    public ClientTreeNode createNode(Object context);
}
