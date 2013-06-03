package lsfusion.client.descriptor;

import lsfusion.client.descriptor.property.CalcPropertyDescriptor;
import lsfusion.client.descriptor.property.PropertyInterfaceDescriptor;

import java.util.Map;

public class CalcPropertyObjectDescriptor extends PropertyObjectDescriptor {

    public CalcPropertyObjectDescriptor() {
    }

    public CalcPropertyObjectDescriptor(CalcPropertyDescriptor property, Map<PropertyInterfaceDescriptor, ? extends PropertyObjectInterfaceDescriptor> mapping) {
        super(property, mapping);
    }

    public CalcPropertyObjectDescriptor(PropertyDescriptorImplement<PropertyObjectInterfaceDescriptor> implement) {
        super(implement);
    }
}
