package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.identity.IDGenerator;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectView extends IdentityObject implements ServerIdentitySerializable {

    public ObjectEntity entity;
    
    private GroupObjectView groupObject;

    public ObjectView() {
    }

    public ObjectView(IDGenerator idGen, ObjectEntity entity, GroupObjectView groupTo) {
        super(entity.getID(), entity.getSID());

        this.entity = entity;
        this.groupObject = groupTo;
    }

    public LocalizedString getCaption() {
        return entity.getCaption();
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.serializeObject(outStream, groupObject);
        pool.writeString(outStream, ThreadLocalContext.localize(entity.caption));

        entity.baseClass.serialize(outStream);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        entity = pool.context.entity.getObject(ID);
    }

    public void finalizeAroundInit() {
    }

    // copy-constructor
    public ObjectView(ObjectView src, ObjectMapping mapping) {
        super(src);
        mapping.put(src, this);

        this.entity = mapping.get(src.entity);
        this.groupObject = mapping.get(src.groupObject);
        this.ID = entity.getID(); //need to be in copy, not in constructor, because entity is copied in constructor
    }
}
