package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.interop.form.object.AbstractGroupObject;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFSet;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.filter.FilterControlsView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectView extends ArrayList<ObjectView> implements ServerIdentitySerializable, PropertyGroupContainerView, AbstractGroupObject<ComponentView, LocalizedString> {

    private IDGenerator idGen;
    public GroupObjectEntity entity;

    public GridView grid;
    public ToolbarView toolbarSystem;
    public NFSet<FilterView> filters;
    public ImSet<FilterView> getFilters() {
        return filters.getSet();
    }
    public Iterable<FilterView> getFiltersIt() {
        return filters.getIt();
    }
    public ContainerView filtersContainer;
    public FilterControlsView filterControls; 
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
        this.idGen = idGen;
        this.entity = entity;

        for (ObjectEntity object : this.entity.getObjects())
            add(new ObjectView(idGen, object, this));

        grid = new GridView(idGen.idShift(), this);
        toolbarSystem = new ToolbarView(idGen.idShift());

        filtersContainer = new ContainerView(idGen.idShift());
        filtersContainer.setType(ContainerType.CONTAINERH);
//        filtersContainer.setType(ContainerType.CONTAINERV);
//        filtersContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT);
        filtersContainer.setAlignment(FlexAlignment.STRETCH);
//        filtersContainer.setAlignCaptions(true);
//        filtersContainer.setLineSize(0);
//        filtersContainer.setCaption(LocalizedString.create(ThreadLocalContext.localize("{form.view.filters.container}")));

        filterControls = new FilterControlsView(idGen.idShift());
        filterControls.setAlignment(FlexAlignment.STRETCH);
        
        filters = NFFact.orderSet();
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
    public ContainerView getFiltersContainer() {
        return filtersContainer;
    }

    @Override
    public FilterControlsView getFilterControls() {
        return filterControls;
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
    
    public FilterView addFilter(PropertyDrawView property, Version version) {
        FilterView filter = new FilterView(idGen.idShift(), property);
        filters.add(filter, version);
        filtersContainer.add(filter, version);
        return filter;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeObject(outStream, entity.viewType);
        pool.writeObject(outStream, entity.listViewType);
        pool.writeObject(outStream, entity.pivotOptions);
        pool.writeString(outStream, entity.customRenderFunction);
        pool.writeString(outStream, entity.mapTileProvider);
        pool.writeBoolean(outStream, entity.asyncInit);
        pool.serializeCollection(outStream, this);
        pool.serializeObject(outStream, pool.context.view.getTreeGroup(entity.treeGroup));

        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, toolbarSystem);
        pool.serializeObject(outStream, filtersContainer);
        pool.serializeObject(outStream, filterControls);
        pool.serializeCollection(outStream, getFilters());
        pool.serializeObject(outStream, calculations);

        outStream.writeBoolean(entity.isParent != null);
        outStream.writeBoolean(pool.context.view.entity.isMap(entity));
        outStream.writeBoolean(pool.context.view.entity.isCalendarDate(entity));
        outStream.writeBoolean(pool.context.view.entity.isCalendarDateTime(entity));
        outStream.writeBoolean(pool.context.view.entity.isCalendarPeriod(entity));

        outStream.writeBoolean(pool.context.view.hasHeaders(entity));
        outStream.writeBoolean(pool.context.view.entity.hasFooters(entity));

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
        filtersContainer = pool.deserializeObject(inStream);
        filterControls = pool.deserializeObject(inStream);
        filters = NFFact.finalSet(pool.deserializeSet(inStream));
        calculations = pool.deserializeObject(inStream);

        needVerticalScroll = inStream.readBoolean();
    }

    public void finalizeAroundInit() {
        grid.finalizeAroundInit();
        toolbarSystem.finalizeAroundInit();
        for (FilterView filter : getFiltersIt()) {
            filter.finalizeAroundInit();
        }
        calculations.finalizeAroundInit();
        
        for(ObjectView object : this) 
            object.finalizeAroundInit();
    }
}
