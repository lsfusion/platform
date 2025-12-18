package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerFactory;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.IdentityView;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public class GroupObjectView extends IdentityView<GroupObjectView, GroupObjectEntity> {

    public GroupObjectEntity entity;

    public ImOrderSet<ObjectView> objects;

    public GridView grid;

    public Boolean needVerticalScroll = true; //todo: no setter in Proxy

    @Override
    public int getID() {
        return entity.getID();
    }

    public String getSID() {
        return entity.getSID();
    }

    @Override
    public String toString() {
        return entity.toString();
    }

    public GroupObjectView(IDGenerator idGen, ContainerFactory<ContainerView> containerFactory, GroupObjectEntity entity, Version version) {
        this.entity = entity;
        this.entity.view = this;

        objects = entity.getOrderObjects().mapOrderSetValues(object -> new ObjectView(object, this));

        grid = new GridView(idGen, containerFactory, this, version);
    }

    public LocalizedString getContainerCaption() {
        if (!objects.isEmpty())
            return objects.get(0).entity.getCaption();
        else
            return null;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeObject(outStream, entity.viewType);
        pool.writeObject(outStream, entity.listViewType);
        pool.writeObject(outStream, entity.pivotOptions);
        pool.writeString(outStream, entity.customRenderFunction);
        pool.writeString(outStream, entity.mapTileProvider);
        pool.writeBoolean(outStream, entity.asyncInit);
        pool.serializeCollection(outStream, objects);
        pool.serializeObject(outStream, pool.context.view.getTreeGroup(entity.treeGroup));

        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, grid.toolbarSystem);
        pool.serializeObject(outStream, grid.filtersContainer);
        pool.serializeObject(outStream, grid.filterControls);
        pool.serializeCollection(outStream, grid.getFilters());
        pool.serializeObject(outStream, grid.calculations);

        outStream.writeBoolean(entity.isParent != null);
        outStream.writeBoolean(pool.context.entity.isMap(entity));
        outStream.writeBoolean(pool.context.entity.isCalendarDate(entity));
        outStream.writeBoolean(pool.context.entity.isCalendarDateTime(entity));
        outStream.writeBoolean(pool.context.entity.isCalendarPeriod(entity));

        outStream.writeBoolean(pool.context.entity.hasHeaders(entity));
        outStream.writeBoolean(pool.context.entity.hasFooters(entity));

        boolean needVScroll;
        if (needVerticalScroll == null) {
            needVScroll = (entity.pageSize != null && entity.pageSize == 0);
        } else {
            needVScroll = needVerticalScroll;
        }
        pool.writeInt(outStream, entity.pageSize);
        outStream.writeBoolean(needVScroll);
        outStream.writeBoolean(entity.isEnableManualUpdate());
        outStream.writeUTF(getSID());
    }

    public void finalizeAroundInit() {
        grid.finalizeAroundInit();

        for(ObjectView object : objects)
            object.finalizeAroundInit();
    }

    // copy-constructor
    public GroupObjectView(GroupObjectView src, ObjectMapping mapping) {
        super(src, mapping);

        needVerticalScroll = src.needVerticalScroll;

        entity = mapping.get(src.entity);
        grid = mapping.get(src.grid);
        objects = mapping.get(src.objects);
    }
    // no extend and add

    @Override
    public GroupObjectEntity getAddParent(ObjectMapping mapping) {
        return entity;
    }
    @Override
    public GroupObjectView getAddChild(GroupObjectEntity groupObjectEntity, ObjectMapping mapping) {
        return groupObjectEntity.view;
    }
    @Override
    public GroupObjectView copy(ObjectMapping mapping) {
        return new GroupObjectView(this, mapping);
    }
}
