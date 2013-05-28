package platform.client.descriptor.nodes;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.tree.ClientTree;
import platform.base.context.ApplicationContext;
import platform.base.context.ApplicationContextProvider;

import javax.swing.*;

public class GroupObjectFolder extends PlainTextNode<GroupObjectFolder> implements ApplicationContextProvider {

    private FormDescriptor form;

    public ApplicationContext getContext() {
        return form.getContext();
    }

    public GroupObjectFolder(FormDescriptor form) {
        super(ClientResourceBundle.getString("descriptor.editor.group.objectgroups"));

        this.form = form;

        for (GroupObjectDescriptor descriptor : form.groupObjects) {
            add(new GroupObjectNode(descriptor, form));
        }

        addCollectionReferenceActions(form, "groupObjects", new String[]{""}, new Class[]{GroupObjectDescriptor.class});
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return ClientTree.getNode(info) instanceof GroupObjectNode;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return form.moveGroupObject((GroupObjectDescriptor) ClientTree.getNode(info).getTypedObject(), ClientTree.getChildIndex(info));
    }
}
