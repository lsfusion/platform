package platform.client.logics;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPropertyValueLink extends ClientValueLink {

    public ClientPropertyView property;

    public String toString() { return "Свойство"; }

    byte getTypeID() {
        return 2;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(property.ID);
    }
}
