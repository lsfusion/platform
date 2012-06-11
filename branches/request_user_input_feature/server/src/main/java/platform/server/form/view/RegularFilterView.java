package platform.server.form.view;

import platform.base.identity.IdentityObject;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegularFilterView extends IdentityObject implements ServerIdentitySerializable {

    public RegularFilterEntity entity;

    @SuppressWarnings({"UnusedDeclaration"})
    public RegularFilterView() {

    }
    
    public RegularFilterView(RegularFilterEntity entity) {
        ID = entity.ID;
        this.entity = entity;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeString(outStream, entity.name);
        pool.writeObject(outStream, entity.key);
        outStream.writeBoolean(entity.showKey);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        entity = pool.context.entity.getRegularFilter(ID);
    }
}
