package lsfusion.client.descriptor.nodes;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.ObjectDescriptor;
import lsfusion.client.descriptor.editor.ObjectEditor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;
import lsfusion.client.tree.ClientTree;

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
