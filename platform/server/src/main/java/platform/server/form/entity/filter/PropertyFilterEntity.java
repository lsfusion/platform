package platform.server.form.entity.filter;

import platform.interop.serialization.SerializationPool;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.logics.property.PropertyInterface;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.entity.PropertyObjectEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public abstract class PropertyFilterEntity<P extends PropertyInterface> extends FilterEntity {

    public PropertyObjectEntity<P> property;

    public PropertyFilterEntity(PropertyObjectEntity<P> iProperty) {
        property = iProperty;
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        property.fillObjects(objects);
    }

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, property);
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo: ??
    }
}
