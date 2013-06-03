package lsfusion.client.descriptor;

import lsfusion.client.descriptor.property.ActionPropertyDescriptor;
import lsfusion.client.descriptor.property.PropertyInterfaceDescriptor;

import java.util.Map;

public class ActionPropertyDescriptorImplement<T> extends PropertyDescriptorImplement {
    public ActionPropertyDescriptorImplement(ActionPropertyDescriptor property, Map<PropertyInterfaceDescriptor, T> mapping) {
        super(property, mapping);
    }

    public ActionPropertyDescriptorImplement(ActionPropertyDescriptorImplement<T> implement) {
        super(implement);
    }
}
