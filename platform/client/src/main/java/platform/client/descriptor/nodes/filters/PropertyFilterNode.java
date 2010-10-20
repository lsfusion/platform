package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.PropertyFilterDescriptor;

public class PropertyFilterNode extends FilterNode<PropertyFilterDescriptor, PropertyFilterNode>{

    public PropertyFilterNode(GroupObjectDescriptor group, PropertyFilterDescriptor descriptor) {
        super(group, descriptor);
    }
}
