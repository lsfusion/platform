package platform.client.descriptor.nodes;

import platform.client.ClientTree;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.ObjectEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public class ObjectNode extends DescriptorNode<ObjectDescriptor, ObjectNode> implements EditingTreeNode {

    private GroupObjectDescriptor group;

    public ObjectNode(ObjectDescriptor userObject, GroupObjectDescriptor group) {
        super(userObject, false);

        this.group = group;
    }

    public JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new ObjectEditor(getTypedObject());
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
