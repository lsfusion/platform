package platform.client.descriptor;

import platform.client.logics.ClientComponent;

import java.util.List;

public interface ContainerMovable {

    GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList);
    ClientComponent getClientComponent();
    
}
