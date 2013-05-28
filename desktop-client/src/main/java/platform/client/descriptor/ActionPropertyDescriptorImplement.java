package platform.client.descriptor;

import platform.client.descriptor.property.ActionPropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;

import java.util.Map;

public class ActionPropertyDescriptorImplement<T> extends PropertyDescriptorImplement {
    public ActionPropertyDescriptorImplement(ActionPropertyDescriptor property, Map<PropertyInterfaceDescriptor, T> mapping) {
        super(property, mapping);
    }

    public ActionPropertyDescriptorImplement(ActionPropertyDescriptorImplement<T> implement) {
        super(implement);
    }
}
