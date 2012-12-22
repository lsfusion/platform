package platform.server.form.view;

import platform.base.identity.IDGenerator;
import platform.server.form.entity.ObjectEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectView implements ServerIdentitySerializable {

    public ObjectEntity entity;
    private GroupObjectView groupObject;

    public ClassChooserView classChooser;

    public ObjectView() {

    }

    public ObjectView(IDGenerator idGen, ObjectEntity entity, GroupObjectView groupTo) {

        this.entity = entity;
        this.groupObject = groupTo;

        classChooser = new ClassChooserView(idGen.idShift(), this.entity, this);
    }

    public ObjectView(IDGenerator idGen, ObjectEntity entity, GroupObjectView groupTo, boolean clChooser) {

        this.entity = entity;
        this.groupObject = groupTo;

        classChooser = new ClassChooserView(idGen.idShift(), this.entity, this);
    }

    public int getID() {
        return entity.ID;
    }

    int ID;

    public void setID(int iID) {
        ID = iID;
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
}
