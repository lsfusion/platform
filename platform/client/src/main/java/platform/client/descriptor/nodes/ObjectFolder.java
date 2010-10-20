package platform.client.descriptor.nodes;

import platform.base.BaseUtils;
import platform.client.ClientTree;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.nodes.actions.AddableTreeNode;
import platform.client.logics.ClientObject;

import javax.swing.*;
import javax.swing.tree.TreePath;

public class ObjectFolder extends PlainTextNode<ObjectFolder> implements AddableTreeNode {
    private final GroupObjectDescriptor group;

    public ObjectFolder(GroupObjectDescriptor group) {
        super("Oбъекты");
        this.group = group;

        for (ObjectDescriptor object : group) {
            add(new ObjectNode(object, group));
        }
    }

    public Object[] addNewElement(TreePath selectionPath) {
        ClientObject clientObject = new ClientObject();
        clientObject.groupObject = group.client;

        ObjectDescriptor object = new ObjectDescriptor();
        object.client = clientObject;

        group.addObject(object);

        return BaseUtils.add(ClientTree.convertTreePathToUserObjects(selectionPath), object);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return ClientTree.getNode(info) instanceof ObjectNode;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return group.moveObject((ObjectDescriptor) ClientTree.getNode(info).getTypedObject(), ClientTree.getChildIndex(info));
    }

}
