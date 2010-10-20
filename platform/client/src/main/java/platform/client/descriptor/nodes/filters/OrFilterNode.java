package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.filter.OrFilterDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.nodes.NullFieldNode;
import platform.client.descriptor.nodes.actions.NewElementListener;

public class OrFilterNode extends FilterNode<OrFilterDescriptor, OrFilterNode> {

    public OrFilterNode(GroupObjectDescriptor group, final OrFilterDescriptor descriptor) {
        super(group, descriptor);

        if (descriptor.op1 != null)
            add(descriptor.op1.getNode(group));
        else {

            NullFieldNode op1Node = new NullFieldNode("Фильтр 1");
            FilterDescriptor.addNewElementActions(op1Node, new NewElementListener<FilterDescriptor>() {
                public void newElement(FilterDescriptor element) {
                    descriptor.op1 = element;
                    IncrementDependency.update(this, "filters");
                }
            });
            add(op1Node);
        }

        if (descriptor.op2 != null)
            add(descriptor.op2.getNode(group));
        else {
            NullFieldNode op2Node = new NullFieldNode("Фильтр 2");
            FilterDescriptor.addNewElementActions(op2Node, new NewElementListener<FilterDescriptor>() {
                public void newElement(FilterDescriptor element) {
                    descriptor.op2 = element;
                    IncrementDependency.update(this, "filters");
                }
            });
            add(op2Node);
        }
    }
}
