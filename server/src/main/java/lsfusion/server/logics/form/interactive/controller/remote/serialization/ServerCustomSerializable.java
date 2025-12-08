package lsfusion.server.logics.form.interactive.controller.remote.serialization;

import lsfusion.interop.form.remote.serialization.CustomSerializable;

import java.io.DataInputStream;

public interface ServerCustomSerializable extends CustomSerializable<ServerSerializationPool> {

    default void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) {
        throw new UnsupportedOperationException();
    }
}
