package lsfusion.client.descriptor;

import lsfusion.client.descriptor.property.PropertyDescriptor;
import lsfusion.client.descriptor.property.PropertyInterfaceDescriptor;

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
