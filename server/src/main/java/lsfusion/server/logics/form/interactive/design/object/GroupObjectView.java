package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.object.AbstractGroupObject;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.base.version.interfaces.NFSet;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.filter.FilterControlsView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectView extends ArrayList<ObjectView> implements ServerIdentitySerializable, PropertyGroupContainerView, AbstractGroupObject<ComponentView, LocalizedString> {

    private IDGenerator idGen;
    public GroupObjectEntity entity;

    public GridView grid;
    public ToolbarView toolbarSystem;
    public NFList<FilterView> filters = NFFact.list();
    public ImList<FilterView> getFilters() {
        return filters.getList();
    }
    public Iterable<FilterView> getFiltersIt() {
        return filters.getIt();
    }
    public ContainerView filtersContainer;
    public FilterControlsView filterControls; 
    public CalculationsView calculations;

    public Boolean needVerticalScroll = true; //todo: no setter in Proxy

    private int ID;

    public ObjectView getObjectView(ObjectEntity object) {
        for (ObjectView view : this) {
            if (view.entity.equals(object)) {
                return view;
            }
        }
        return null;
    }

    public GroupObjectView(IDGenerator idGen, GroupObjectEntity entity, Version version) {
        this.entity = entity;
        this.entity.view = this;

        this.idGen = idGen;

        for (ObjectEntity object : this.entity.getObjects())
            add(new ObjectView(idGen, object, this));

        grid = new GridView(idGen.idShift(), idGen.idShift(), this);
        toolbarSystem = new ToolbarView(idGen.idShift());

        filtersContainer = new ContainerView(idGen.idShift());
        if (Settings.get().isVerticalColumnsFiltersContainer()) {
            filtersContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT, version);
        } else {
            filtersContainer.setHorizontal(true, version);
        }
        //disable isReversed optimisation for FILTERS container because children are added after isReversed check
        filtersContainer.setReversed(false, version);

        // behaves weirdly if unset as alignCaptions property sometimes depends on children count, which changes in runtime for filters container
        filtersContainer.setAlignCaptions(false, version);
        
//        filtersContainer.setLineSize(0);
//        filtersContainer.setCaption(LocalizedString.create(ThreadLocalContext.localize("{form.view.filters.container}")));

        filterControls = new FilterControlsView(idGen.idShift());

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

    public BaseComponentView getGrid() {
        return grid;
    }

    @Override
    public BaseComponentView getToolbarSystem() {
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
    public BaseComponentView getCalculations() {
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

    @Override
    public String getPropertyGroupContainerName() {
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
        outStream.writeBoolean(pool.context.entity.isMap(entity));
        outStream.writeBoolean(pool.context.entity.isCalendarDate(entity));
        outStream.writeBoolean(pool.context.entity.isCalendarDateTime(entity));
        outStream.writeBoolean(pool.context.entity.isCalendarPeriod(entity));

        outStream.writeBoolean(pool.context.view.hasHeaders(entity));
        outStream.writeBoolean(pool.context.entity.hasFooters(entity));

        boolean needVScroll;
        if (needVerticalScroll == null) {
            needVScroll = (entity.pageSize != null && entity.pageSize == 0);
        } else {
            needVScroll = needVerticalScroll;
        }
        pool.writeInt(outStream, entity.pageSize);
        outStream.writeBoolean(needVScroll);
        outStream.writeBoolean(entity.enableManualUpdate);
        outStream.writeUTF(getSID());
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

    public DefaultFormView.ContainerSet containers;

    @Override
    public DefaultFormView.ContainerSet getContainers() {
        return containers;
    }

    // copy-constructor
    public GroupObjectView(GroupObjectView src, ObjectMapping mapping) {
        mapping.put(src, this);

        entity = mapping.get(src.entity);
        entity.view = this;

        idGen = src.idGen;
        needVerticalScroll = src.needVerticalScroll;

        ID = entity.getID();
        grid = mapping.get(src.grid);
        toolbarSystem = mapping.get(src.toolbarSystem);

        filters.add(src.filters, mapping::get, mapping.version);

        filtersContainer = mapping.get(src.filtersContainer);
        filterControls = mapping.get(src.filterControls);
        calculations = mapping.get(src.calculations);

        for(ObjectView objectView : src)
            add(mapping.get(objectView));

        if(!entity.isInTree())
            containers = new DefaultFormView.ContainerSet(src.containers, mapping);
    }
}
