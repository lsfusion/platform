package platform.client.descriptor.nodes;

import javax.swing.tree.DefaultMutableTreeNode;

public class DescriptorNode<T> extends DefaultMutableTreeNode {

    public DescriptorNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public DescriptorNode(Object userObject) {
        super(userObject);
    }

    T getDescriptor() {
        return (T)super.getUserObject();
    }
}
