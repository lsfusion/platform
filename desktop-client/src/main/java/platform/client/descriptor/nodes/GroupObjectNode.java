package platform.client.descriptor.nodes;

import platform.client.Main;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.GroupObjectEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.tree.ClientTree;

import javax.swing.*;

public class GroupObjectNode extends DescriptorNode<GroupObjectDescriptor, GroupObjectNode> implements EditableTreeNode {
    private static final ImageIcon groupObjectIcon = new ImageIcon(Main.class.getResource("/images/groupObject.png"));

    private FormDescriptor form;

    public GroupObjectNode(GroupObjectDescriptor group, FormDescriptor form) {
        super(group);

        this.form = form;

        add(new ObjectFolder(group));

        GroupElementFolder.addFolders(this, group, form);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new GroupObjectEditor(getTypedObject(), form);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return getSiblingNode(info) != null;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return form.moveGroupObject(getSiblingNode(info).getTypedObject(), getTypedObject());
    }

    @Override
    public Icon getIcon() {
        return groupObjectIcon;
    }
}
