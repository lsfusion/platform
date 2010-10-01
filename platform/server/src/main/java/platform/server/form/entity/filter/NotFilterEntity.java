package platform.server.form.entity.filter;

import platform.interop.serialization.SerializationPool;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.NotFilterInstance;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

public class NotFilterEntity extends FilterEntity {

    public FilterEntity filter;

    public NotFilterEntity(FilterEntity filter) {
        this.filter = filter;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        filter.fillObjects(objects);
    }

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, filter);
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:
    }
}
