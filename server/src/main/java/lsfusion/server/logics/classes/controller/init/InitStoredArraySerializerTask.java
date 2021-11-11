package lsfusion.server.logics.classes.controller.init;

import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.implementations.stored.StoredArraySerializerRegistry;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import org.apache.log4j.Logger;

public class InitStoredArraySerializerTask extends SimpleBLTask {
    @Override
    public String getCaption() {
        return "Adding serialization methods to StoredArraySerializer";
    }

    @Override
    public void run(Logger logger) {
        StoredArraySerializerRegistry serializer = (StoredArraySerializerRegistry) StoredArraySerializer.getInstance(); 
        serializer.register(ObjectEntity.class, ObjectEntity::serialize, ObjectEntity::deserialize);
    }
}
