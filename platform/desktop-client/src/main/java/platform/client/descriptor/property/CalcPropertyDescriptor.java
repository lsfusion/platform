package platform.client.descriptor.property;

import platform.client.descriptor.CalcPropertyObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;

import java.util.Map;

public class CalcPropertyDescriptor extends PropertyDescriptor {
    public CalcPropertyDescriptor() {

    }

    @Override
    public PropertyObjectDescriptor createPropertyObject(Map<PropertyInterfaceDescriptor, ObjectDescriptor> mapping) {
        return new CalcPropertyObjectDescriptor(this, mapping);
    }
}
