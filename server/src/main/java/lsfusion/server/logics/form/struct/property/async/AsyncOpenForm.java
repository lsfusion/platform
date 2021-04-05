package lsfusion.server.logics.form.struct.property.async;

import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncOpenForm extends AsyncExec {
    public String canonicalName;
    public String caption;
    public boolean forbidDuplicate;
    public boolean modal;

    public AsyncOpenForm(String canonicalName, String caption, boolean forbidDuplicate, boolean modal) {
        this.canonicalName = canonicalName;
        this.caption = caption;
        this.forbidDuplicate = forbidDuplicate;
        this.modal = modal;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeString(outStream, canonicalName);
        pool.writeString(outStream, caption);
        pool.writeBoolean(outStream, forbidDuplicate);
        pool.writeBoolean(outStream, modal);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        SerializationUtil.writeString(outStream, canonicalName);
        SerializationUtil.writeString(outStream, caption);
        outStream.writeBoolean(forbidDuplicate);
        outStream.writeBoolean(modal);
    }
}