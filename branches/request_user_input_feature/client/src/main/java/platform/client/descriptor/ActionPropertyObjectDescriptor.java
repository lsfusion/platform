package platform.client.descriptor;

import platform.client.descriptor.property.ActionPropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;

import java.util.Map;

public class ActionPropertyObjectDescriptor extends PropertyObjectDescriptor {

    public ActionPropertyObjectDescriptor() {
    }

    public ActionPropertyObjectDescriptor(ActionPropertyDescriptor property, Map<PropertyInterfaceDescriptor, ? extends PropertyObjectInterfaceDescriptor> mapping) {
        super(property, mapping);
    }

    public ActionPropertyObjectDescriptor(ActionPropertyDescriptorImplement<PropertyObjectInterfaceDescriptor> implement) {
        super(implement);
    }
}
