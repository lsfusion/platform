package platform.client.descriptor.nodes.actions;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

public interface EditingTreeNode {
    NodeEditor createEditor(FormDescriptor form, RemoteDescriptorInterface remote);
}
