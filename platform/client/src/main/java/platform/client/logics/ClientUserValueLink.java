package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.ClientResourceBundle;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientUserValueLink extends ClientValueLink {

    public Object value;

    public String toString() { return ClientResourceBundle.getString("logics.value"); }

    byte getTypeID() {
        return 0;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        BaseUtils.serializeObject(outStream, value);
    }
}
