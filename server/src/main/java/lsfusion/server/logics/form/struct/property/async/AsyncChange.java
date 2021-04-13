package lsfusion.server.logics.form.struct.property.async;

import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncChange extends AsyncInputExec {
    public DataClass changeType;
    public boolean hasList;
    public LP targetProp;

    public AsyncChange(DataClass changeType, boolean hasList, LP targetProp) {
        this.changeType = changeType;
        this.hasList = hasList;
        this.targetProp = targetProp;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        TypeSerializer.serializeType(outStream, changeType);
        pool.writeBoolean(outStream, hasList);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }
}