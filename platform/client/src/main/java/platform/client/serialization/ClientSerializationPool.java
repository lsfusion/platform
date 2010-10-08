package platform.client.serialization;

import platform.client.descriptor.*;
import platform.client.descriptor.filter.*;
import platform.client.descriptor.property.PropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.logics.ClientForm;
import platform.interop.serialization.SerializationPool;


public class ClientSerializationPool extends SerializationPool<ClientForm> {


    public ClientSerializationPool() {
        this(null);
    }

    public ClientSerializationPool(ClientForm context) {
        super(context);
        //порядок добавления должен соответствовать порядку в ServerSerializationPool

        addMapping(FormDescriptor.class);
        addMapping(GroupObjectDescriptor.class);
        addMapping(PropertyDrawDescriptor.class);
        addMapping(PropertyDescriptor.class);

        addMapping(PropertyFilterDescriptor.class);
        addMapping(CompareFilterDescriptor.class);
        addMapping(IsClassFilterDescriptor.class);
        addMapping(NotNullFilterDescriptor.class);

        addMapping(OrFilterDescriptor.class);
        addMapping(NotFilterDescriptor.class);

        addMapping(RegularFilterDescriptor.class);
        addMapping(RegularFilterGroupDescriptor.class);

        addMapping(PropertyInterfaceDescriptor.class);

        addMapping(DataObjectDescriptor.class);
        addMapping(CurrentComputerDescriptor.class);
        addMapping(ObjectDescriptor.class);
        addMapping(PropertyObjectDescriptor.class);
    }
}
