package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.server.base.thread.ThreadLocalContext;
import lsfusion.server.logics.form.interactive.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;

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

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        String name = ThreadLocalContext.localize(entity.name);
        pool.writeString(outStream, name);
        pool.writeObject(outStream, entity.key);
        outStream.writeBoolean(entity.showKey);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        entity = pool.context.entity.getRegularFilter(ID);
    }
}
