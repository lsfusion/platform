package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientFunctionView extends ClientComponentView {

    public ClientFunctionView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);
    }
}
