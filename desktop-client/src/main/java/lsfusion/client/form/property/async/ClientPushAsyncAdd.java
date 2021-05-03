package lsfusion.client.form.property.async;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPushAsyncAdd extends ClientPushAsyncResult {
    
    public final long id;

    public ClientPushAsyncAdd(long id) {
        this.id = id;
    }

    @Override
    protected void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeLong(id);
    }
}
