package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;

import javax.swing.tree.DefaultMutableTreeNode;

public class FormNode extends DefaultMutableTreeNode {

    public FormNode(FormDescriptor descriptor) {
        super(descriptor);

        add(new GroupObjectFolder(descriptor.groups, descriptor.propertyDraws));
        add(new FixedFilterFolder(descriptor.fixedFilters));
    }
}
