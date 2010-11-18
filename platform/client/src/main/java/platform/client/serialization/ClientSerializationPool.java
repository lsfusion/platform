package platform.client.serialization;

import platform.client.descriptor.*;
import platform.client.descriptor.filter.*;
import platform.client.descriptor.property.AbstractGroupDescriptor;
import platform.client.descriptor.property.PropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.logics.*;
import platform.base.context.ApplicationContext;
import platform.base.serialization.SerializationPool;


public class ClientSerializationPool extends SerializationPool<ClientForm> {


    public ClientSerializationPool() {
        this(null, null);
    }

    public ClientSerializationPool(ApplicationContext appContext) {
        this(null, appContext);
    }

    public ClientSerializationPool(ClientForm context, ApplicationContext appContext) {
        super(context, appContext);
        //порядок добавления должен соответствовать порядку в ServerSerializationPool

        addMapping(FormDescriptor.class);
        addMapping(GroupObjectDescriptor.class);
        addMapping(PropertyDrawDescriptor.class);
        addMapping(PropertyDescriptor.class);
        addMapping(AbstractGroupDescriptor.class);

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
        addMapping(ObjectDescriptor.class);
        addMapping(PropertyObjectDescriptor.class);

        addMapping(ClientForm.class);
        addMapping(ClientComponent.class);
        addMapping(ClientContainer.class);
        addMapping(ClientGroupObject.class);
        addMapping(ClientShowType.class);
        addMapping(ClientGrid.class);
        addMapping(ClientClassChooser.class);
        addMapping(ClientObject.class);
        addMapping(ClientPropertyDraw.class);
        addMapping(ClientRegularFilter.class);
        addMapping(ClientRegularFilterGroup.class);
        addMapping(ClientFunction.class);
    }
}
