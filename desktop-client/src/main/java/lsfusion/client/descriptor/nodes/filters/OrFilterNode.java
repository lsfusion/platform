package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.filter.FilterDescriptor;
import lsfusion.client.descriptor.filter.OrFilterDescriptor;

public class OrFilterNode extends FilterNode<OrFilterDescriptor, OrFilterNode> {

    public OrFilterNode(GroupObjectDescriptor group, final OrFilterDescriptor descriptor) {
        super(group, descriptor);

        addFieldReferenceNode(descriptor, "op1", ClientResourceBundle.getString("descriptor.filter")+" 1", group, FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
        addFieldReferenceNode(descriptor, "op2", ClientResourceBundle.getString("descriptor.filter")+" 2", group, FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
    }
}
