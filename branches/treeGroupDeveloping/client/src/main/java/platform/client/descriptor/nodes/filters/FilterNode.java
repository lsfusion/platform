package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.nodes.GroupElementNode;

public abstract class FilterNode<T extends FilterDescriptor, C extends FilterNode> extends GroupElementNode<T, C> {

    public FilterNode(GroupObjectDescriptor group, T userObject) {
        super(group, userObject);
    }
}
