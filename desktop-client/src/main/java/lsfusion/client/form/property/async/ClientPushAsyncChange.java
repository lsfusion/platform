package lsfusion.client.form.property.async;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.interop.form.property.cell.UserInputResult;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.serializeObject;

public class ClientPushAsyncChange extends ClientPushAsyncResult {
    
    public final UserInputResult result;

    public ClientPushAsyncChange(Object value) {
        this(new UserInputResult(value));
    }

    public ClientPushAsyncChange(UserInputResult result) {
        this.result = result;
    }

    @Override
    protected void serialize(DataOutputStream outStream) throws IOException {
        BaseUtils.writeObject(outStream, result);
    }
}
