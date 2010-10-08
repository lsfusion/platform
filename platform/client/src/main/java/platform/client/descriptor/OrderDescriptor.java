package platform.client.descriptor;

import platform.interop.serialization.CustomSerializable;
import platform.client.serialization.ClientCustomSerializable;

import java.util.List;

public interface OrderDescriptor extends ClientCustomSerializable {

    GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groups);
}
