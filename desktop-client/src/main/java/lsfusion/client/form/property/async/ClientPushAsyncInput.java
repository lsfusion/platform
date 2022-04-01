package lsfusion.client.form.property.async;

import lsfusion.base.BaseUtils;
import lsfusion.interop.form.property.cell.UserInputResult;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPushAsyncInput extends ClientPushAsyncResult {
    
    public final UserInputResult result;

    public ClientPushAsyncInput(Object value, Integer contextAction) {
        this(new UserInputResult(value, contextAction));
    }

    public ClientPushAsyncInput(UserInputResult result) {
        this.result = result;
    }

    @Override
    protected void serialize(DataOutputStream outStream) throws IOException {
        BaseUtils.writeObject(outStream, result);
    }
}
