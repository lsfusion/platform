package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.interop.form.object.table.tree.AbstractTreeGroup;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFSet;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TreeGroupView extends ComponentView implements ServerIdentitySerializable, PropertyGroupContainerView, AbstractTreeGroup<ComponentView> {
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
    
    public boolean expandOnClick = true;

    public int headerHeight = -1;

    public Integer lineWidth;
    public Integer lineHeight;

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
        filtersContainer.setType(ContainerType.CONTAINERH);
//        filtersContainer.setType(ContainerType.CONTAINERV);
//        filtersContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT);
        filtersContainer.setAlignment(FlexAlignment.STRETCH);
//        filtersContainer.setAlignCaptions(true);
//        filtersContainer.setLineSize(0);
        filtersContainer.setCaption(LocalizedString.create(ThreadLocalContext.localize("{form.view.filters.container}")));

        filters = NFFact.orderSet();
    }

    @Override
    public double getDefaultFlex(FormEntity formEntity) {
        return 1;
    }

    @Override
    public FlexAlignment getDefaultAlignment(FormEntity formEntity) {
        return FlexAlignment.STRETCH;
    }

    @Override
    public ComponentView getToolbarSystem() {
        return toolbarSystem;
    }

    @Override
    public ContainerView getFiltersContainer() {
        return filtersContainer;
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

        pool.serializeCollection(outStream, groups);
        pool.serializeObject(outStream, toolbarSystem);
        pool.serializeObject(outStream, filtersContainer);
        pool.serializeCollection(outStream, getFilters());

        outStream.writeBoolean(entity.plainTreeMode);
        
        outStream.writeBoolean(expandOnClick);

        outStream.writeInt(headerHeight);

        outStream.writeInt(getLineWidth());
        outStream.writeInt(getLineHeight());
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        autoSize = inStream.readBoolean();

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

    public int getLineWidth() {
        if(lineWidth != null)
            return lineWidth;

        return -1;
    }

    public int getLineHeight() {
        if(lineHeight != null)
            return lineHeight;

        return -1;
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        toolbarSystem.finalizeAroundInit();
        for (FilterView filter : getFiltersIt()) {
            filter.finalizeAroundInit();
        }
    }

    public Boolean autoSize;

    public boolean isAutoSize(FormEntity entity) {
        if(autoSize != null)
            return autoSize;

        return false;
    }

    @Override
    protected int getDefaultWidth(FormEntity entity) {
        return -2;
    }

    @Override
    protected int getDefaultHeight(FormEntity entity) {
        return -2;
    }
}
