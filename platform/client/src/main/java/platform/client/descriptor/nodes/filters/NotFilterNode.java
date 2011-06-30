package platform.client.descriptor.nodes.filters;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.filter.NotFilterDescriptor;
import platform.client.descriptor.nodes.actions.NewElementListener;

public class NotFilterNode extends FilterNode<NotFilterDescriptor, NotFilterNode> implements NewElementListener<FilterDescriptor> {

    private NotFilterDescriptor descriptor;

    public NotFilterNode(GroupObjectDescriptor group, NotFilterDescriptor descriptor) {
        super(group, descriptor);

        this.descriptor = descriptor;

        addFieldReferenceNode(descriptor, "filter", ClientResourceBundle.getString("descriptor.filter"), group, FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
    }

    public void newElement(FilterDescriptor filter) {
        descriptor.filter = filter;
        descriptor.updateDependency(this, "filters");
    }
}
