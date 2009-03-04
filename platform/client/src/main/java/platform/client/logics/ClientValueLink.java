package platform.client.logics;

import java.io.DataOutputStream;
import java.io.IOException;

abstract public class ClientValueLink {

    abstract byte getTypeID();
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());
    }    
}
