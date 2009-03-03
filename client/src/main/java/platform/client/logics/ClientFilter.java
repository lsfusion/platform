package platform.client.logics;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientFilter {

    public ClientPropertyView property;
    public ClientValueLink value;

    public int compare;

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(property.ID);

        outStream.writeInt(compare);

        value.serialize(outStream);
    }    
}
