package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;

public abstract class GroupElementNode<T, C extends GroupElementNode> extends DescriptorNode<T, C> implements EditableTreeNode {

    protected GroupObjectDescriptor groupObject;

    public GroupElementNode(GroupObjectDescriptor groupObject, T userObject) {
        super(userObject, true);
        
        this.groupObject = groupObject;
    }
}

