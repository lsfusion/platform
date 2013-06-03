package lsfusion.client.serialization;

import lsfusion.base.serialization.IdentitySerializable;

//вообще должно быть так:
//public interface ClientIdentitySerializable extends ClientCustomSerializable, IdentitySerializable<ClientSerializationPool> {
//но при компиляции AspectJ выдаёт кучу ошибок cyclic dependency/hierarchy inconsistent
public interface ClientIdentitySerializable extends IdentitySerializable<ClientSerializationPool> {
}
