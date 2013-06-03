package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.filter.FilterDescriptor;
import lsfusion.client.descriptor.nodes.GroupElementNode;

public abstract class FilterNode<T extends FilterDescriptor, C extends FilterNode> extends GroupElementNode<T, C> {

    public FilterNode(GroupObjectDescriptor group, T userObject) {
        super(group, userObject);
    }
}
