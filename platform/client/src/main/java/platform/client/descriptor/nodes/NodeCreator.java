package platform.client.descriptor.nodes;

import platform.client.ClientTreeNode;

public interface NodeCreator {
    public ClientTreeNode createNode(Object context);
}
