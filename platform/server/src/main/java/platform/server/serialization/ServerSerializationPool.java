package platform.server.serialization;

import platform.interop.serialization.SerializationPool;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.FilterEntity;

public class ServerSerializationPool extends SerializationPool {
    public ServerSerializationPool() {
        this(null);
    }
    
    public ServerSerializationPool(Object context) {
        super(context);
        //порядок добавления должен соответствовать порядку в ClientSerializationPool

        addMapping(FormEntity.class);
        addMapping(GroupObjectEntity.class);
        addMapping(PropertyDrawEntity.class);
        addMapping(FilterEntity.class);
    }
}
