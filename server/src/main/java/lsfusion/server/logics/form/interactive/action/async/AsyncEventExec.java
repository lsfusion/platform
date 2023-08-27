package lsfusion.server.logics.form.interactive.action.async;

import com.google.common.base.Throwables;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class AsyncEventExec {

    public abstract byte getTypeId();

    public void serialize(ConnectionContext context, DataOutputStream dataOutputStream) throws IOException {
    }

    public PushAsyncResult deserializePush(byte[] value) {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(value));
        try {
            return deserializePush(inStream);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public PushAsyncResult deserializePush(DataInputStream outStream) throws IOException {
        return null;
    }
}