package lsfusion.server.form.view;

import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.layout.AbstractGroupObject;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectView extends ArrayList<ObjectView> implements ServerIdentitySerializable, AbstractGroupObject<ComponentView> {

    public GroupObjectEntity entity;

    public GridView grid;
    public ShowTypeView showType;
    public ToolbarView toolbar;
    public FilterView filter;

    public Boolean needVerticalScroll = true;

    private int ID;

    public GroupObjectView() {
    }

    public ObjectView getObjectView(ObjectEntity object) {
        for (ObjectView view : this) {
            if (view.entity.equals(object)) {
                return view;
            }
        }
        return null;
    }

    public GroupObjectView(IDGenerator idGen, GroupObjectEntity entity) {
        this.entity = entity;

        for (ObjectEntity object : this.entity.getObjects())
            add(new ObjectView(idGen, object, this));

        grid = new GridView(idGen.idShift(), this);
        showType = new ShowTypeView(idGen.idShift(), this);
        toolbar = new ToolbarView(idGen.idShift());
        filter = new FilterView(idGen.idShift());
    }

    public String getCaption() {
        if (size() > 0)
            return ThreadLocalContext.localize(get(0).getCaption());
        else
            return null;
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

    @Override
    public ComponentView getToolbar() {
        return toolbar;
    }

    @Override
    public ComponentView getFilter() {
        return filter;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public String getSID() {
        return entity.getSID();
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, entity.banClassView);
        pool.serializeCollection(outStream, this, serializationType);
        pool.serializeObject(outStream, pool.context.view.getTreeGroup(entity.treeGroup));

        pool.serializeObject(outStream, grid, serializationType);
        pool.serializeObject(outStream, showType, serializationType);
        pool.serializeObject(outStream, toolbar, serializationType);
        pool.serializeObject(outStream, filter, serializationType);

        outStream.writeBoolean(entity.isParent != null);

        boolean needVScroll;
        if (needVerticalScroll == null) {
            needVScroll = (entity.pageSize != null && entity.pageSize == 0);
        } else {
            needVScroll = needVerticalScroll;
        }
        pool.writeInt(outStream, entity.pageSize);
        outStream.writeBoolean(needVScroll);
        outStream.writeUTF(getSID());
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        entity = pool.context.entity.getGroupObject(ID);

        pool.deserializeCollection(this, inStream);

        grid = pool.deserializeObject(inStream);
        showType = pool.deserializeObject(inStream);
        toolbar = pool.deserializeObject(inStream);
        filter = pool.deserializeObject(inStream);

        needVerticalScroll = inStream.readBoolean();
    }

    public void finalizeAroundInit() {
        grid.finalizeAroundInit();
        showType.finalizeAroundInit();
        toolbar.finalizeAroundInit();
        filter.finalizeAroundInit();
        
        for(ObjectView object : this) 
            object.finalizeAroundInit();
    }
}
