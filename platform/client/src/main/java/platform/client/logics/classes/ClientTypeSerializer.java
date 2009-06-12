package platform.client.logics.classes;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientTypeSerializer {

    public static ClientType deserialize(DataInputStream inStream) throws IOException {
        if(inStream.readBoolean())
            return ClientObjectClass.type;
        else
            return (ClientDataClass)(ClientClass.deserialize(inStream));
    }

}
