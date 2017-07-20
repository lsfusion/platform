package lsfusion.server.form.view;

import lsfusion.base.identity.IdentityObject;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;

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
        // todo [dale]: нужно протянуть нужную локаль  
        String name = ThreadLocalContext.localize(entity.name);
        pool.writeString(outStream, name);
        pool.writeObject(outStream, entity.key);
        outStream.writeBoolean(entity.showKey);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        entity = pool.context.entity.getRegularFilter(ID);
    }
}
