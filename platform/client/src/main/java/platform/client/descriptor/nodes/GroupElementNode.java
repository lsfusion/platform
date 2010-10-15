package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.nodes.actions.EditingTreeNode;

public abstract class GroupElementNode<T, C extends GroupElementNode> extends DescriptorNode<T, C> implements EditingTreeNode {

    protected GroupObjectDescriptor groupObject;

    public GroupElementNode(GroupObjectDescriptor groupObject, T userObject) {
        super(userObject, true);
        
        this.groupObject = groupObject;
    }
}

