package platform.server.form.view;

import platform.base.IDGenerator;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectView extends ArrayList<ObjectView> implements ServerIdentitySerializable {

    public GroupObjectEntity entity;

    public GroupObjectView() {
        
    }
    
    public GroupObjectView(IDGenerator idGen, GroupObjectEntity entity) {
        this.entity = entity;

        for(ObjectEntity object : this.entity)
            add(new ObjectView(idGen, object, this));
        
        grid = new GridView(idGen.idShift(), this);
        showType = new ShowTypeView(idGen.idShift(), this);
    }

    public GridView grid;
    public ShowTypeView showType;

    public int getID() {
        return entity.getID();
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeByte(entity.banClassView);
        pool.serializeCollection(outStream, this);
        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, showType);
    }

    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(this, inStream);

        grid = pool.deserializeObject(inStream);
        showType = pool.deserializeObject(inStream);

        entity = pool.context.form.getGroupObject(iID);
    }
}
