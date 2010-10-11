package platform.client.descriptor.editor;

import platform.client.descriptor.*;
import platform.client.descriptor.property.PropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.serialization.ClientSerializationPool;
import platform.client.logics.classes.ClientClass;
import platform.interop.serialization.RemoteDescriptorInterface;
import platform.base.BaseUtils;

import javax.swing.*;
import java.util.*;
import java.io.*;

public class PropertyObjectEditor extends JComboBox {

    public PropertyObjectEditor(GroupObjectDescriptor groupObject, FormDescriptor form, RemoteDescriptorInterface remote, PropertyObjectDescriptor property) {
        // узнаем все текущие классы для groupObject и все верхние группы
        // и передаем

        Collection<PropertyObjectDescriptor> properties = form.getProperties(groupObject, remote);
        for(PropertyObjectDescriptor availableProperty : properties)
            addItem(availableProperty);
        setSelectedItem(property);
    }
}
