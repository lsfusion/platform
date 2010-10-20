package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.OrFilterDescriptor;

public class OrFilterNode extends FilterNode<OrFilterDescriptor, OrFilterNode> {

    public OrFilterNode(GroupObjectDescriptor group, OrFilterDescriptor descriptor) {
        super(group, descriptor);

        add(descriptor.op1.getNode(group));
        add(descriptor.op2.getNode(group));
    }
}
