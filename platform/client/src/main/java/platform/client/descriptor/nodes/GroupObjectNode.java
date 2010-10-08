package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.GroupObjectEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public class GroupObjectNode extends DescriptorNode<GroupObjectDescriptor> implements EditingTreeNode {

    public GroupObjectNode(GroupObjectDescriptor group, FormDescriptor form) {
        super(group);

        add(new ObjectFolder(group));

        GroupElementFolder.addFolders(this, group, form);
    }

    public JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new GroupObjectEditor(getDescriptor());
    }
}
