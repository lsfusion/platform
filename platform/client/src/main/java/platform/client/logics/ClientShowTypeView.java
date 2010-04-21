package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientShowTypeView extends ClientComponentView {

    ClientShowTypeView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);
    }
}
