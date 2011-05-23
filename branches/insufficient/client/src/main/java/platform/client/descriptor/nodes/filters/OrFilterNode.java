package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.filter.OrFilterDescriptor;

public class OrFilterNode extends FilterNode<OrFilterDescriptor, OrFilterNode> {

    public OrFilterNode(GroupObjectDescriptor group, final OrFilterDescriptor descriptor) {
        super(group, descriptor);

        addFieldReferenceNode(descriptor, "op1", "Фильтр 1", group, FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
        addFieldReferenceNode(descriptor, "op2", "Фильтр 2", group, FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
    }
}
