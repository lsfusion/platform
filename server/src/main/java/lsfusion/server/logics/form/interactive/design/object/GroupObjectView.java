package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.object.AbstractGroupObject;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectView extends ArrayList<ObjectView> implements ServerIdentitySerializable, PropertyGroupContainerView, AbstractGroupObject<ComponentView, LocalizedString> {

    public GroupObjectEntity entity;

    public GridView grid;
    public ToolbarView toolbarSystem;
    public FilterView userFilter;
    public CalculationsView calculations;

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
        toolbarSystem = new ToolbarView(idGen.idShift());
        userFilter = new FilterView(idGen.idShift());
        calculations = new CalculationsView(idGen.idShift()); 
    }

    public LocalizedString getCaption() {
        if (size() > 0)
            return get(0).getCaption();
        else
            return null;
    }

    public int getID() {
        return entity.getID();
    }

    public ComponentView getGrid() {
        return grid;
    }

    @Override
    public ComponentView getToolbarSystem() {
        return toolbarSystem;
    }

    @Override
    public ComponentView getUserFilter() {
        return userFilter;
    }

    @Override
    public ComponentView getCalculations() {
        return calculations;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public String getSID() {
        return entity.getSID();
    }

    @Override
    public String getPropertyGroupContainerSID() {
        return getSID();
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeObject(outStream, entity.viewType);
        pool.writeObject(outStream, entity.listViewType);
        pool.writeObject(outStream, entity.pivotOptions);
        pool.writeString(outStream, entity.functionToExecute != null ? entity.functionToExecute : "");
        pool.writeBoolean(outStream, entity.asyncInit);
        pool.serializeCollection(outStream, this);
        pool.serializeObject(outStream, pool.context.view.getTreeGroup(entity.treeGroup));

        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, toolbarSystem);
        pool.serializeObject(outStream, userFilter);
        pool.serializeObject(outStream, calculations);

        outStream.writeBoolean(entity.isParent != null);
        outStream.writeBoolean(pool.context.view.entity.isMap(entity));

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
        toolbarSystem = pool.deserializeObject(inStream);
        userFilter = pool.deserializeObject(inStream);
        calculations = pool.deserializeObject(inStream);

        needVerticalScroll = inStream.readBoolean();
    }

    public void finalizeAroundInit() {
        grid.finalizeAroundInit();
        toolbarSystem.finalizeAroundInit();
        userFilter.finalizeAroundInit();
        calculations.finalizeAroundInit();
        
        for(ObjectView object : this) 
            object.finalizeAroundInit();
    }
}
