package platform.client.descriptor.nodes;

import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.editor.PropertyDrawEditor;

import javax.swing.*;

public class PropertyDrawNode extends DescriptorNode<PropertyDrawDescriptor> implements EditingTreeNode {

    public PropertyDrawNode(Object userObject) {
        super(userObject, false);
    }

    public JComponent createEditor() {
        return new PropertyDrawEditor(getDescriptor());
    }
}
