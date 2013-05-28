package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.tree.ClientTree;
import platform.base.context.ApplicationContext;
import platform.base.context.ApplicationContextProvider;

import javax.swing.*;

public class ObjectFolder extends PlainTextNode<ObjectFolder> implements ApplicationContextProvider {

    private final GroupObjectDescriptor group;

    public ApplicationContext getContext() {
        return group.getContext();
    }

    public ObjectFolder(GroupObjectDescriptor group) {
        super("Oбъекты");
        this.group = group;

        for (ObjectDescriptor object : group.objects) {
            add(new ObjectNode(object, group));
        }
        addCollectionReferenceActions(group, "objects", new String[]{""}, new Class[]{ObjectDescriptor.class});
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
