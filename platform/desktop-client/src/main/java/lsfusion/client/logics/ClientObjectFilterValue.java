package lsfusion.client.logics;

import lsfusion.client.ClientResourceBundle;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientObjectFilterValue extends ClientFilterValue {

    public ClientObject object;

    public String toString() { return ClientResourceBundle.getString("logics.object"); }

    byte getTypeID() {
        return 1;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(object.getID());
    }
}
