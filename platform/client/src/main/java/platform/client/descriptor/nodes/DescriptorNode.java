package platform.client.descriptor.nodes;


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
