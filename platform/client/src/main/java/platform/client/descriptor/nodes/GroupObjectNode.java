package platform.client.descriptor.nodes;

import platform.client.ClientTree;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.GroupObjectEditor;
import platform.client.descriptor.nodes.actions.EditingTreeNode;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public class GroupObjectNode extends DescriptorNode<GroupObjectDescriptor, GroupObjectNode> implements EditingTreeNode {

    private FormDescriptor form;

    public GroupObjectNode(GroupObjectDescriptor group, FormDescriptor form) {
        super(group);

        this.form = form;

        add(new ObjectFolder(group));

        GroupElementFolder.addFolders(this, group, form);
    }

    public JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new GroupObjectEditor(getTypedObject());
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return getSiblingNode(info) != null;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return form.moveGroupObject(getSiblingNode(info).getTypedObject(), getTypedObject());
    }
}
