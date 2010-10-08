package platform.client.descriptor.nodes;

import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.PropertyDrawEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PropertyDrawNode extends GroupElementNode<PropertyDrawDescriptor> {

    public PropertyDrawNode(GroupObjectDescriptor groupObject, PropertyDrawDescriptor userObject) {
        super(groupObject, userObject);
    }

    public JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new PropertyDrawEditor(groupObject, getDescriptor(), form, remote);
    }
}
