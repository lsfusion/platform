package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.interop.form.object.table.tree.AbstractTreeGroup;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFSet;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.filter.FilterControlsView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.physics.admin.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TreeGroupView extends GridPropertyView implements ServerIdentitySerializable, PropertyGroupContainerView, AbstractTreeGroup<ComponentView> {
    public static final String TREE_PREFIX = "TREE";
    
    public List<GroupObjectView> groups = new ArrayList<>();

    public TreeGroupEntity entity;

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
    
    public boolean expandOnClick = true;

    IDGenerator idGenerator;

    @Override
    public String getPropertyGroupContainerSID() {
        return TREE_PREFIX + " " + entity.getSID();
    }

    public TreeGroupView() {
        
    }

    public TreeGroupView(FormView form, TreeGroupEntity entity, Version version) {
        super(entity.getID());

        this.entity = entity;

        for (GroupObjectEntity group : entity.getGroups()) {
            groups.add(form.getNFGroupObject(group, version));
        }

        idGenerator = form.idGenerator;
        toolbarSystem = new ToolbarView(idGenerator.idShift());

        filtersContainer = new ContainerView(idGenerator.idShift());
        if (Settings.get().isVerticalColumnsFiltersContainer()) {
            filtersContainer.setType(ContainerType.CONTAINERV);
            filtersContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT);
        } else {
            filtersContainer.setType(ContainerType.CONTAINERH);
        }
        filtersContainer.setAlignment(FlexAlignment.STRETCH);
//        filtersContainer.setAlignCaptions(true);
//        filtersContainer.setLineSize(0);
//        filtersContainer.setCaption(LocalizedString.create(ThreadLocalContext.localize("{form.view.filters.container}")));

        filterControls = new FilterControlsView(idGenerator.idShift());
        filterControls.setAlignment(FlexAlignment.END);

        filters = NFFact.orderSet();
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

    public void add(GroupObjectView group) {
        groups.add(group);
    }

    public FilterView addFilter(PropertyDrawView property, Version version) {
        FilterView filterView = new FilterView(idGenerator.idShift(), property);
        filters.add(filterView, version);
        filtersContainer.add(filterView, version);
        return filterView;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(isAutoSize(pool.context.entity));
        outStream.writeBoolean(boxed != null);
        if(boxed != null)
            outStream.writeBoolean(boxed);

        pool.serializeCollection(outStream, groups);
        pool.serializeObject(outStream, toolbarSystem);
        pool.serializeObject(outStream, filtersContainer);
        pool.serializeObject(outStream, filterControls);
        pool.serializeCollection(outStream, getFilters());

        outStream.writeBoolean(entity.plainTreeMode);
        
        outStream.writeBoolean(expandOnClick);

        outStream.writeInt(headerHeight);

        outStream.writeBoolean(resizeOverflow != null);
        if(resizeOverflow != null)
            outStream.writeBoolean(resizeOverflow);

        outStream.writeInt(getLineWidth());
        outStream.writeInt(getLineHeight());
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        autoSize = inStream.readBoolean();
        boxed = inStream.readBoolean() ? inStream.readBoolean() : null;

        groups = pool.deserializeList(inStream);
        toolbarSystem = pool.deserializeObject(inStream);
        filtersContainer = pool.deserializeObject(inStream);
        filters = NFFact.finalSet(pool.deserializeSet(inStream));

        expandOnClick = inStream.readBoolean();

        headerHeight = inStream.readInt();

        lineWidth = inStream.readInt();
        lineHeight = inStream.readInt();

        entity = pool.context.entity.getTreeGroup(ID);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        toolbarSystem.finalizeAroundInit();
        for (FilterView filter : getFiltersIt()) {
            filter.finalizeAroundInit();
        }
    }

    @Override
    protected boolean isCustom() {
        return false;
    }
}
