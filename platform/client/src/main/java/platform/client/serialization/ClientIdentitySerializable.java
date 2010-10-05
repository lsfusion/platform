package platform.client.serialization;

import platform.interop.serialization.IdentitySerializable;

public interface ClientIdentitySerializable extends ClientCustomSerializable, IdentitySerializable<ClientSerializationPool> {
}
