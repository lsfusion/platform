package lsfusion.server.form.entity.filter;

import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public abstract class OpFilterEntity<This extends OpFilterEntity<This>> extends FilterEntity {

    public FilterEntity op1;
    public FilterEntity op2;

    protected OpFilterEntity() {
    }

    public OpFilterEntity(FilterEntity op1, FilterEntity op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        op1.fillObjects(objects);
        op2.fillObjects(objects);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, op1);
        pool.serializeObject(outStream, op2);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        op1 = (FilterEntity) pool.deserializeObject(inStream);
        op2 = (FilterEntity) pool.deserializeObject(inStream);
    }
}
