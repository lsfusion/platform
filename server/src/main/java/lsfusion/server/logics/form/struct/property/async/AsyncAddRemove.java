package lsfusion.server.logics.form.struct.property.async;

import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncAddRemove implements AsyncInputExec {
    public ObjectEntity object;
    public boolean add;

    public AsyncAddRemove(ObjectEntity object, boolean add) {
        this.object = object;
        this.add = add;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.serializeObject(outStream, pool.context.view.getObject(object));
        outStream.writeBoolean(add);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }
}