package platform.server.form.view;

import platform.base.IDGenerator;
import platform.server.form.entity.ObjectEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectView implements ClientSerialize, ServerIdentitySerializable {

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

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(entity.getID());
        outStream.writeUTF(entity.caption);
        outStream.writeBoolean(entity.addOnTransaction);

        entity.baseClass.serialize(outStream);
        classChooser.serialize(outStream);
    }

    public int getID() {
        return entity.ID;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupObject);
        pool.writeString(outStream, entity.caption);

        outStream.writeBoolean(entity.addOnTransaction);

        entity.baseClass.serialize(outStream);
        pool.serializeObject(outStream, classChooser);
    }

    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        //todo:
    }
}
