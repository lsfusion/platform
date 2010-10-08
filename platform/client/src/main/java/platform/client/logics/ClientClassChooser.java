package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientClassChooser extends ClientComponent {

    public ClientClassChooser() {
        
    }
    
    public ClientClassChooser(DataInputStream inStream, Collection<ClientContainer> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);
    }
}
