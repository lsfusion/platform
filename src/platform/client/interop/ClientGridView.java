package platform.client.interop;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientGridView extends ClientComponentView {

    public ClientGridView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);
    }
}
