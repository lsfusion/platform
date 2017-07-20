package lsfusion.base.serialization;

import lsfusion.base.identity.IdentityInterface;

public interface IdentitySerializable<P extends SerializationPool> extends CustomSerializable<P>, IdentityInterface {
}
