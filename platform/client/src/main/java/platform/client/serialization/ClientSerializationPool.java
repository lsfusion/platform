package platform.client.serialization;

import platform.client.descriptor.FormDescriptor;
import platform.interop.serialization.SerializationPool;

public class ClientSerializationPool extends SerializationPool {
    public ClientSerializationPool() {
        //порядок добавления должен соответствовать порядку в ServerSerializationPool

        addMapping(FormDescriptor.class);
    }
}
