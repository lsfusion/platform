package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientClassView extends ClientComponentView {

    public boolean show;

    public ClientClassView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        show = inStream.readBoolean();
    }
}
