package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.NotFilterDescriptor;

public class NotFilterNode extends FilterNode<NotFilterDescriptor, NotFilterNode> {

    public NotFilterNode(GroupObjectDescriptor group, NotFilterDescriptor descriptor) {
        super(group, descriptor);

        add(descriptor.filter.getNode(group));
    }
}
