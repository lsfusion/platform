package platform.client.descriptor.nodes;

import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.editor.ObjectEditor;

import javax.swing.*;

public class ObjectNode extends DescriptorNode<ObjectDescriptor> implements EditingTreeNode {

    public ObjectNode(Object userObject) {
        super(userObject, false);
    }

    public JComponent createEditor() {
        return new ObjectEditor(getDescriptor());
    }
}
