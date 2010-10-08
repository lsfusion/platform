package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class GroupElementNode<T> extends DescriptorNode<T> implements EditingTreeNode {

    protected GroupObjectDescriptor groupObject;

    public GroupElementNode(GroupObjectDescriptor groupObject, T userObject) {
        super(userObject, false);
        
        this.groupObject = groupObject;
    }
}

