package lsfusion.client.descriptor.nodes;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.editor.FormEditor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;
import lsfusion.client.tree.ClientTreeNode;

public class FormNode extends ClientTreeNode<FormDescriptor, FormNode> implements EditableTreeNode {
    private final FormDescriptor descriptor;
    public final GroupObjectFolder groupObjectFolder;

    public FormNode(FormDescriptor descriptor) {
        super(descriptor);
        this.descriptor = descriptor;

        groupObjectFolder = new GroupObjectFolder(descriptor);
        add(groupObjectFolder);

        add(new TreeGroupFolder(descriptor));

        GroupElementFolder.addFolders(this, null, descriptor);
        add(new LayoutFolder(descriptor.client));
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new FormEditor(descriptor);
    }
}
