package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.PropertyFilterDescriptor;

public abstract class PropertyFilterNode<T extends PropertyFilterDescriptor, C extends PropertyFilterNode> extends FilterNode<T, C>{

    public PropertyFilterNode(GroupObjectDescriptor group, T descriptor) {
        super(group, descriptor);
    }
}
