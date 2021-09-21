package lsfusion.client.form.property.async;

import com.google.common.base.Throwables;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class ClientPushAsyncResult {
    
    // should be consistent with AsyncEventExec.deserializePush
    public byte[] serialize() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serialize(dataStream);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return outStream.toByteArray();
    }

    protected abstract void serialize(DataOutputStream outStream) throws IOException;
}
