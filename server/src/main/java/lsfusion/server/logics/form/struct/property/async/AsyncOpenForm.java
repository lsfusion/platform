package lsfusion.server.logics.form.struct.property.async;

import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncOpenForm extends AsyncExec {
    public String caption;
    public boolean modal;

    public AsyncOpenForm(String caption, boolean modal) {
        this.caption = caption;
        this.modal = modal;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeString(outStream, caption);
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
        SerializationUtil.writeString(outStream, caption);
        outStream.writeBoolean(modal);
    }
}