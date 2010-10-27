package platform.client.descriptor.nodes;

import platform.client.descriptor.editor.FormEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.tree.ClientTreeNode;
import platform.client.descriptor.FormDescriptor;

public class FormNode extends ClientTreeNode<FormDescriptor, FormNode> implements EditableTreeNode {
    private final FormDescriptor descriptor;

    public FormNode(FormDescriptor descriptor) {
        super(descriptor);
        this.descriptor = descriptor;

        add(new GroupObjectFolder(descriptor));

        GroupElementFolder.addFolders(this, null, descriptor);
        add(new LayoutFolder(descriptor.client));
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new FormEditor(descriptor);
    }
}
