package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.editor.ObjectEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.tree.ClientTree;

import javax.swing.*;

public class ObjectNode extends DescriptorNode<ObjectDescriptor, ObjectNode> implements EditableTreeNode {

    private GroupObjectDescriptor group;

    public ObjectNode(ObjectDescriptor userObject, GroupObjectDescriptor group) {
        super(userObject, false);

        this.group = group;
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new ObjectEditor(getTypedObject(), form);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return getSiblingNode(info) != null;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return group.moveObject(getSiblingNode(info).getTypedObject(), getTypedObject());
    }
}
