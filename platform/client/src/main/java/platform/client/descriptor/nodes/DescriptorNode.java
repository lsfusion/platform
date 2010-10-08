package platform.client.descriptor.nodes;


public class DescriptorNode<T> extends ClientTreeNode {

    public DescriptorNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public DescriptorNode(Object userObject) {
        super(userObject);
    }

    T getDescriptor() {
        return (T) super.getUserObject();
    }
}
