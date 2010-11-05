package platform.client.descriptor;

import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;

import java.util.List;

public interface ContainerMovable {

    ClientContainer getDestinationContainer(ClientContainer parent, List<GroupObjectDescriptor> groupObjects);
    ClientComponent getClientComponent(ClientContainer parent);
    
}
