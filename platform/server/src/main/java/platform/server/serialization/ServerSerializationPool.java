package platform.server.serialization;

import platform.interop.serialization.SerializationPool;
import platform.server.form.entity.FormEntity;

public class ServerSerializationPool extends SerializationPool {
    public ServerSerializationPool() {
        //порядок добавления должен соответствовать порядку в ClientSerializationPool

        addMapping(FormEntity.class);
    }
}
