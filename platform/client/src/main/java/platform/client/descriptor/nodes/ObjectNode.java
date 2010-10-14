package platform.client.descriptor.nodes;

import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.ObjectEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public class ObjectNode extends DescriptorNode<ObjectDescriptor, ObjectNode> implements EditingTreeNode {

    public ObjectNode(ObjectDescriptor userObject) {
        super(userObject, false);
    }

    public JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new ObjectEditor(getTypedObject());
    }
}
