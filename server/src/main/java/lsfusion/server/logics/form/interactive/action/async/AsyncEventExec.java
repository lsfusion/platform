package lsfusion.server.logics.form.interactive.action.async;

import com.google.common.base.Throwables;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

public abstract class AsyncEventExec {

    public abstract byte getTypeId();

    public void serialize(ConnectionContext context, DataOutputStream dataOutputStream) throws IOException {
    }

    public static PushAsyncResult deserializePush(byte[] value, Supplier<? extends AsyncEventExec> getAsyncExec) {
        if (value == null)
            return null;
        AsyncEventExec asyncExec = getAsyncExec.get();
        return asyncExec != null ? asyncExec.deserializePush(value) : null;
    }

    public PushAsyncResult deserializePush(byte[] value) {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(value));
        try {
            // a push is read only by a prediction of its own kind: a client can send one the server does not
            // predict (a custom view pushes its value whatever the handler is), and a decoder must not read it
            if (inStream.readByte() != getTypeId())
                return null;
            return deserializePush(inStream);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public PushAsyncResult deserializePush(DataInputStream outStream) throws IOException {
        return null;
    }
}
