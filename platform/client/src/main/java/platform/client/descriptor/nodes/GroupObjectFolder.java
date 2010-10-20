package platform.client.descriptor.nodes;

import platform.base.BaseUtils;
import platform.client.ClientTree;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.nodes.actions.AddableTreeNode;
import platform.client.logics.ClientGroupObject;

import javax.swing.*;
import javax.swing.tree.TreePath;

public class GroupObjectFolder extends PlainTextNode<GroupObjectFolder> implements AddableTreeNode {

    private FormDescriptor form;

    public GroupObjectFolder(FormDescriptor form) {
        super("Группы объектов");

        this.form = form;

        for (GroupObjectDescriptor descriptor : form.groupObjects) {
            add(new GroupObjectNode(descriptor, form));
        }
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return ClientTree.getNode(info) instanceof GroupObjectNode;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return form.moveGroupObject((GroupObjectDescriptor)ClientTree.getNode(info).getTypedObject(), ClientTree.getChildIndex(info));
    }

    public Object[] addNewElement(TreePath selectionPath) {
        ClientGroupObject clientGroup = new ClientGroupObject();

        GroupObjectDescriptor group = new GroupObjectDescriptor();
        group.client = clientGroup;

        form.addGroupObject(group);

        return BaseUtils.add( ClientTree.convertTreePathToUserObjects(selectionPath), group );
    }
}
