package platform.client.descriptor;

import platform.client.descriptor.property.PropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;

import java.util.Map;

public class PropertyDescriptorImplement<T> {

    public PropertyDescriptor property;
    public Map<PropertyInterfaceDescriptor, T> mapping;

    public PropertyDescriptorImplement() {
    }

    public PropertyDescriptorImplement(PropertyDescriptor property, Map<PropertyInterfaceDescriptor, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public PropertyDescriptorImplement(PropertyDescriptorImplement<T> implement) {
        property = implement.property;
        mapping = implement.mapping;
    }
}
