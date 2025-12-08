package lsfusion.server.logics.form.interactive.controller.remote.serialization;

import lsfusion.interop.form.remote.serialization.IdentitySerializable;

import java.io.DataInputStream;

public interface ServerIdentitySerializable extends ServerCustomSerializable, IdentitySerializable<ServerSerializationPool> {

    default void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) {
        throw new UnsupportedOperationException();
    }
}
