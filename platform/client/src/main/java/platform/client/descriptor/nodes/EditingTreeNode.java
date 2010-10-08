package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public interface EditingTreeNode {
    JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote);
}
