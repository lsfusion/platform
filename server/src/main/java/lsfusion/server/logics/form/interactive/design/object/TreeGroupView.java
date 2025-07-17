package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.object.table.tree.AbstractTreeGroup;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFSet;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
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
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
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
    public int hierarchicalWidth;
    public String hierarchicalCaption;
    public PropertyObjectEntity propertyHierarchicalCaption;

    @Override
    protected boolean hasPropertyComponent() {
        return super.hasPropertyComponent() || propertyHierarchicalCaption != null;
    }

    IDGenerator idGenerator;

    @Override
    public String getPropertyGroupContainerSID() {
        return TREE_PREFIX + " " + entity.getSID();
    }

    @Override
    public String getPropertyGroupContainerName() {
        return entity.getSID();
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
            filtersContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT);
        } else {
            filtersContainer.setHorizontal(true);
        }
//        filtersContainer.setAlignCaptions(true);
//        filtersContainer.setLineSize(0);
//        filtersContainer.setCaption(LocalizedString.create(ThreadLocalContext.localize("{form.view.filters.container}")));

        filterControls = new FilterControlsView(idGenerator.idShift());

        filters = NFFact.orderSet();
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

        pool.serializeCollection(outStream, groups);
        pool.serializeObject(outStream, toolbarSystem);
        pool.serializeObject(outStream, filtersContainer);
        pool.serializeObject(outStream, filterControls);
        pool.serializeCollection(outStream, getFilters());

        outStream.writeBoolean(entity.plainTreeMode);
        
        outStream.writeBoolean(expandOnClick);
        outStream.writeInt(hierarchicalWidth);
        pool.writeString(outStream, hierarchicalCaption);
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
