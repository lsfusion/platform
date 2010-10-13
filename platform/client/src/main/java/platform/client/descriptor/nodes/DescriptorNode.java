package platform.client.descriptor.nodes;

import platform.client.ClientTreeNode;

public class DescriptorNode<T> extends ClientTreeNode {

    public DescriptorNode(T userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public DescriptorNode(T userObject) {
        super(userObject);
    }

    T getDescriptor() {
        return (T) super.getUserObject();
    }
}
