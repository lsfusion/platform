package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;

public abstract class GroupElementNode<T, C extends GroupElementNode> extends DescriptorNode<T, C> {

    protected GroupObjectDescriptor groupObject;

    public GroupElementNode(GroupObjectDescriptor groupObject, T userObject) {
        super(userObject, true);
        
        this.groupObject = groupObject;
    }
}

