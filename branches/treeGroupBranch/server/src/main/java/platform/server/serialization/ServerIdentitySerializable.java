package platform.server.serialization;

import platform.interop.serialization.IdentitySerializable;

public interface ServerIdentitySerializable extends ServerCustomSerializable, IdentitySerializable<ServerSerializationPool> {
}
