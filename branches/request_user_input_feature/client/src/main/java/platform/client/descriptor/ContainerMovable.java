package platform.client.descriptor;

import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;

import java.util.List;

public interface ContainerMovable<C extends ClientComponent> {

    ClientContainer getDestinationContainer(ClientContainer parent, List<GroupObjectDescriptor> groupObjects);
    C getClientComponent(ClientContainer parent);
    
}
