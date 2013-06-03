package lsfusion.client.descriptor.property;

import lsfusion.client.descriptor.CalcPropertyObjectDescriptor;
import lsfusion.client.descriptor.ObjectDescriptor;
import lsfusion.client.descriptor.PropertyObjectDescriptor;

import java.util.Map;

public class CalcPropertyDescriptor extends PropertyDescriptor {
    public CalcPropertyDescriptor() {

    }

    @Override
    public PropertyObjectDescriptor createPropertyObject(Map<PropertyInterfaceDescriptor, ObjectDescriptor> mapping) {
        return new CalcPropertyObjectDescriptor(this, mapping);
    }
}
