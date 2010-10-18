package platform.client.descriptor.nodes;

import platform.client.ClientTree;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;

import javax.swing.*;

public class GroupObjectFolder extends PlainTextNode<GroupObjectFolder> {

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
}
