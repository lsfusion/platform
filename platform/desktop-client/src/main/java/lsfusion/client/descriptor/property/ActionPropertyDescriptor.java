package lsfusion.client.descriptor.property;

import lsfusion.client.descriptor.ActionPropertyObjectDescriptor;
import lsfusion.client.descriptor.ObjectDescriptor;
import lsfusion.client.descriptor.PropertyObjectDescriptor;

import java.util.Map;

public class ActionPropertyDescriptor extends PropertyDescriptor {
    public ActionPropertyDescriptor() {

    }

    @Override
    public PropertyObjectDescriptor createPropertyObject(Map<PropertyInterfaceDescriptor, ObjectDescriptor> mapping) {
        return new ActionPropertyObjectDescriptor(this, mapping);
    }
}