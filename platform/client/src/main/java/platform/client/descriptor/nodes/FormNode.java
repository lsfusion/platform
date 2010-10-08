package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;


public class FormNode extends ClientTreeNode {

    public FormNode(FormDescriptor descriptor) {
        super(descriptor);

        add(new GroupObjectFolder(descriptor.groups, descriptor.propertyDraws));
        add(new FixedFilterFolder(descriptor.fixedFilters));
    }
}
