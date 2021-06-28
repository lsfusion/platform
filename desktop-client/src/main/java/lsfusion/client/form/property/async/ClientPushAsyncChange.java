package lsfusion.client.form.property.async;

import lsfusion.base.BaseUtils;
import lsfusion.interop.form.property.cell.UserInputResult;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPushAsyncChange extends ClientPushAsyncResult {
    
    public final UserInputResult result;

    public ClientPushAsyncChange(Object value, Integer contextAction) {
        this(new UserInputResult(value, contextAction));
    }

    public ClientPushAsyncChange(UserInputResult result) {
        this.result = result;
    }

    @Override
    protected void serialize(DataOutputStream outStream) throws IOException {
        BaseUtils.writeObject(outStream, result);
    }
}
