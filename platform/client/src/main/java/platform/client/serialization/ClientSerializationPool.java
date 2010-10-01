package platform.client.serialization;

import platform.client.descriptor.FilterDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.interop.serialization.SerializationPool;

public class ClientSerializationPool extends SerializationPool {
    public ClientSerializationPool() {
        this(null);
    }
    
    public ClientSerializationPool(Object context) {
        super(context);
        //порядок добавления должен соответствовать порядку в ServerSerializationPool

        addMapping(FormDescriptor.class);
        addMapping(GroupObjectDescriptor.class);
        addMapping(PropertyDrawDescriptor.class);
        addMapping(FilterDescriptor.class);
    }
}
