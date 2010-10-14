package platform.client.descriptor.nodes;

import platform.client.ClientTreeNode;

public class DescriptorNode<T, C extends DescriptorNode> extends ClientTreeNode<T, C> {

    public DescriptorNode(T userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public DescriptorNode(T userObject) {
        super(userObject);
    }
}
