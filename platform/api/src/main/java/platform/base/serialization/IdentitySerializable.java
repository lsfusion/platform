package platform.base.serialization;

import platform.base.identity.IdentityInterface;

public interface IdentitySerializable<P extends SerializationPool> extends CustomSerializable<P>, IdentityInterface {
}
