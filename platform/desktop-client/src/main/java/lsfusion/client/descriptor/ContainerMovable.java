package lsfusion.client.descriptor;

import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;

import java.util.List;

public interface ContainerMovable<C extends ClientComponent> {

    ClientContainer getDestinationContainer(ClientContainer parent, List<GroupObjectDescriptor> groupObjects);
    C getClientComponent(ClientContainer parent);
    
}
