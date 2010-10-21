package platform.client.descriptor.nodes;

import platform.client.tree.ClientTreeNode;
import platform.client.descriptor.FormDescriptor;

public class FormNode extends ClientTreeNode<FormDescriptor, FormNode> {

    public FormNode(FormDescriptor descriptor) {
        super(descriptor);

        add(new GroupObjectFolder(descriptor));

        GroupElementFolder.addFolders(this, null, descriptor);
        add(new LayoutFolder(descriptor.client));
    }
}
