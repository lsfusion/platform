package lsfusion.client.form.property.async;

import java.io.DataOutputStream;

public class ClientPushAsyncActivate extends ClientPushAsyncResult {
    @Override
    protected byte getTypeId() {
        return 1;
    }

    @Override
    protected void serialize(DataOutputStream outStream) {
    }
}
