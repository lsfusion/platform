package platform.client.descriptor.editor;

import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public class PropertyDrawEditor extends GroupElementEditor {

    public PropertyDrawEditor(GroupObjectDescriptor groupObject, PropertyDrawDescriptor descriptor, FormDescriptor form, RemoteDescriptorInterface remote) {
        super(groupObject);
        
        add(new JLabel("Property : "+descriptor));
        add(new PropertyObjectEditor(groupObject, form, remote, descriptor.propertyObject));
    }
}
