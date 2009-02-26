package platform.client.interop;

import platform.base.BaseUtils;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientUserValueLink extends ClientValueLink {

    public Object value;

    public String toString() { return "Значение"; }

    byte getTypeID() {
        return 0;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        BaseUtils.serializeObject(outStream, value);
    }
}
