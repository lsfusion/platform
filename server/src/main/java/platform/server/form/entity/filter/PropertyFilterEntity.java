package platform.server.form.entity.filter;

import platform.server.form.entity.CalcPropertyObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public abstract class PropertyFilterEntity<P extends PropertyInterface> extends FilterEntity {

    public CalcPropertyObjectEntity<P> property;
    public boolean resolveAdd;

    // нельзя удалять - используется при сериализации
    protected PropertyFilterEntity() {
    }

    public PropertyFilterEntity(CalcPropertyObjectEntity<P> property, boolean resolveAdd) {
        this.property = property;
        this.resolveAdd = resolveAdd;
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        property.fillObjects(objects);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, property);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        property = pool.deserializeObject(inStream);
    }
}
