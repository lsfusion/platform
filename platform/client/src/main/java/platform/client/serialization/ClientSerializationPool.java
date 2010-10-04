package platform.client.serialization;

import platform.client.descriptor.*;
import platform.client.descriptor.filter.CompareFilterDescriptor;
import platform.client.descriptor.filter.NotFilterDescriptor;
import platform.client.descriptor.filter.OrFilterDescriptor;
import platform.client.descriptor.filter.PropertyFilterDescriptor;
import platform.client.descriptor.property.*;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.logics.ClientForm;
import platform.interop.serialization.SerializationPool;


public class ClientSerializationPool extends SerializationPool {


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
        addMapping(OrFilterDescriptor.class);
        addMapping(NotFilterDescriptor.class);
        addMapping(CompareFilterDescriptor.class);

        addMapping(PropertyInterfaceDescriptor.class);

        addMapping(DataObjectDescriptor.class);
        addMapping(CurrentComputerDescriptor.class);
        addMapping(ObjectDescriptor.class);
        addMapping(PropertyObjectDescriptor.class);

        addMapping(AddObjectActionPropertyDescriptor.class);
        addMapping(ImportFromExcelActionPropertyDescriptor.class);
        addMapping(ObjectValuePropertyDescriptor.class);
        addMapping(SelectionPropertyDescriptor.class);
    }
}
