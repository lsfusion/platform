package platform.client.logics;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientObjectValueLink extends ClientValueLink {

    public ClientObject object;

    public String toString() { return "Объект"; }

    byte getTypeID() {
        return 1;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(object.getID());
    }
}
