package lsfusion.client.form.property.async;

import lsfusion.client.form.property.cell.classes.controller.suggest.CompletionType;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientAsyncSerializer {

    public static ClientAsyncEventExec deserializeEventExec(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();
        switch (type) {
            case 0:
                return null;
            case 1:
                return new ClientAsyncOpenForm(inStream);
            case 2:
                return new ClientAsyncChange(inStream);
            case 3:
                return new ClientAsyncAddRemove(inStream);
        }
        throw new UnsupportedOperationException();
    }

    public static ClientInputList deserializeInputList(DataInputStream inStream) throws IOException {
        String[] actions = SerializationUtil.deserializeArray(inStream);
        ClientAsyncExec[] actionAsyncs = new ClientAsyncExec[actions.length];
        for(int i=0;i<actions.length;i++)
            actionAsyncs[i] = (ClientAsyncExec) deserializeEventExec(inStream);
        return new ClientInputList(actions, actionAsyncs, inStream.readBoolean() ? CompletionType.STRICT : CompletionType.NON_STRICT);
    }

    public static ClientInputList deserializeInputList(byte[] array) throws IOException {
        if(array == null)
            return null;
        return deserializeInputList(new DataInputStream(new ByteArrayInputStream(array)));
    }
}
