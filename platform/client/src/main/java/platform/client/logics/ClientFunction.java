package platform.client.logics;

import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientFunction extends ClientComponent {

    public String caption;

    public ClientFunction() {
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);

        caption = pool.readString(inStream);
    }

    @Override
    public String toString() {
        return caption;
    }
}
