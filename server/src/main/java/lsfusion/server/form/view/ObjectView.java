package lsfusion.server.form.view;

import lsfusion.base.identity.IDGenerator;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectView extends IdentityObject implements ServerIdentitySerializable {

    public ObjectEntity entity;
    
    private GroupObjectView groupObject;

    public ClassChooserView classChooser;

    public ObjectView() {
    }

    public ObjectView(IDGenerator idGen, ObjectEntity entity, GroupObjectView groupTo) {
        super(entity.getID(), entity.getSID());

        this.entity = entity;
        this.groupObject = groupTo;

        classChooser = new ClassChooserView(idGen.idShift(), this.entity, this);
    }

    public String getCaption() {
        return entity.getCaption();
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupObject, serializationType);
        pool.writeString(outStream, entity.caption);

        entity.baseClass.serialize(outStream);
        pool.serializeObject(outStream, classChooser, serializationType);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        classChooser = pool.deserializeObject(inStream);

        entity = pool.context.entity.getObject(ID);
    }

    public void finalizeAroundInit() {
        classChooser.finalizeAroundInit();
    }
}
