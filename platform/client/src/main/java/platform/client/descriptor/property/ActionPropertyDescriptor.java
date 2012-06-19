package platform.client.descriptor.property;

import platform.client.descriptor.ActionPropertyObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;

import java.util.Map;

public class ActionPropertyDescriptor extends PropertyDescriptor {
    public ActionPropertyDescriptor() {

    }

    @Override
    public PropertyObjectDescriptor createPropertyObject(Map<PropertyInterfaceDescriptor, ObjectDescriptor> mapping) {
        return new ActionPropertyObjectDescriptor(this, mapping);
    }
}