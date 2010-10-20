package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.filter.NotFilterDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.nodes.NullFieldNode;
import platform.client.descriptor.nodes.actions.NewElementListener;

public class NotFilterNode extends FilterNode<NotFilterDescriptor, NotFilterNode> implements NewElementListener<FilterDescriptor> {

    private NotFilterDescriptor descriptor;

    public NotFilterNode(GroupObjectDescriptor group, NotFilterDescriptor descriptor) {
        super(group, descriptor);

        this.descriptor = descriptor;

        if (descriptor.filter != null)
            add(descriptor.filter.getNode(group));
        else {
            NullFieldNode propNode = new NullFieldNode("Фильтр");
            FilterDescriptor.addNewElementActions(propNode, this);
            add(propNode);
        }
    }

    public void newElement(FilterDescriptor filter) {
        descriptor.filter = filter;
        IncrementDependency.update(this, "filters");
    }
}
