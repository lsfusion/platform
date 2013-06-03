package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.filter.FilterDescriptor;
import lsfusion.client.descriptor.filter.NotFilterDescriptor;
import lsfusion.client.descriptor.nodes.actions.NewElementListener;

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
