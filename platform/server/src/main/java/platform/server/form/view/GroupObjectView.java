package platform.server.form.view;

import platform.base.IDGenerator;
import platform.interop.form.layout.AbstractGroupObject;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectView extends ArrayList<ObjectView> implements ServerIdentitySerializable, AbstractGroupObject<ComponentView> {

    public GroupObjectEntity entity;

    public GroupObjectView() {
        
    }
    
    public GroupObjectView(IDGenerator idGen, GroupObjectEntity entity) {
        this.entity = entity;

        for(ObjectEntity object : this.entity.objects)
            add(new ObjectView(idGen, object, this));
        
        grid = new GridView(idGen.idShift(), this);
        showType = new ShowTypeView(idGen.idShift(), this);
    }

    public GridView grid;
    public ShowTypeView showType;

    public String getCaption() {
        return get(0).getCaption();
    }

    public int getID() {
        return entity.getID();
    }

    public ComponentView getGrid() {
        return grid;
    }

    public ComponentView getShowType() {
        return showType;
    }

    int ID;
    public void setID(int iID) {
        ID = iID;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, entity.banClassView);
        pool.serializeCollection(outStream, this, serializationType);
        pool.serializeObject(outStream, grid, serializationType);
        pool.serializeObject(outStream, showType, serializationType);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(this, inStream);

        grid = pool.deserializeObject(inStream);
        showType = pool.deserializeObject(inStream);

        entity = pool.context.entity.getGroupObject(ID);
    }
}
