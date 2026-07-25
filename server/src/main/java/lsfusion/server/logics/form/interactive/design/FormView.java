package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.event.*;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;
import lsfusion.server.base.version.interfaces.NFOrderMap;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.base.version.interfaces.NFSet;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.*;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawViewOrPivotColumn;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainersView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.*;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Function;

import static lsfusion.server.logics.form.interactive.design.object.GroupObjectContainerSet.*;

public class FormView<This extends FormView<This>> extends IdentityView<This, FormEntity> {

    public final IDGenerator genID() {
        return entity.genID;
    }

    public FormEntity entity;

    public Integer overridePageWidth;

    // список деревеьев
    protected NFSet<TreeGroupView> treeGroups = NFFact.set();
    public ImSet<TreeGroupView> getTreeGroups() {
        return treeGroups.getSet();
    }
    public Iterable<TreeGroupView> getTreeGroupsIt() {
        return treeGroups.getIt();
    }

    // список групп
    public NFComplexOrderSet<GroupObjectView> groupObjects = NFFact.complexOrderSet();
    public Iterable<GroupObjectView> getGroupObjectsIt() {
        return groupObjects.getIt();
    }
    public ImOrderSet<GroupObjectView> getGroupObjectsListIt() {
        return groupObjects.getOrderSet();
    }
    public Iterable<GroupObjectView> getNFGroupObjectsIt(Version version) {
        return groupObjects.getNFIt(version); 
    }
    public Iterable<GroupObjectView> getNFGroupObjectsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return groupObjects.getNFListIt(version);
    }

    // список свойств
    public NFComplexOrderSet<PropertyDrawView> properties = NFFact.complexOrderSet();
    public Iterable<PropertyDrawView> getPropertiesIt() {
        return properties.getIt();
    }
    public ImOrderSet<PropertyDrawView> getPropertiesList() {
        return properties.getOrderSet();
    }
    public Iterable<PropertyDrawView> getNFPropertiesIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return properties.getNFIt(version);
    }

    // список фильтров
    public NFOrderSet<RegularFilterGroupView> regularFilters = NFFact.orderSet();
    public Iterable<RegularFilterGroupView> getRegularFiltersIt() {
        return regularFilters.getIt();
    }
    public ImOrderSet<RegularFilterGroupView> getRegularFiltersList() {
        return regularFilters.getOrderSet();
    }
    public Iterable<RegularFilterGroupView> getNFRegularFiltersListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return regularFilters.getNFListIt(version);
    }

    protected NFOrderMap<PropertyDrawView,Boolean> defaultOrders = NFFact.orderMap();
    public ImOrderMap<PropertyDrawView, Boolean> getDefaultOrders() {
        return defaultOrders.getListMap();
    }

    public ContainerView mainContainer;

    public TreeGroupView get(TreeGroupEntity treeGroup) { return treeGroup.view; }
    public GroupObjectView get(GroupObjectEntity groupObject) { return groupObject.view; }
    public ObjectView get(ObjectEntity object) { return object.view; }
    public PropertyDrawView get(PropertyDrawEntity property) { return property.view; }
    public FilterView getFilter(PropertyDrawEntity property) { return property.view.filter; }
    public RegularFilterGroupView get(RegularFilterGroupEntity filterGroup) { return filterGroup.view; }

    protected NFOrderSet<ImList<PropertyDrawViewOrPivotColumn>> pivotColumns = NFFact.orderSet();
    public ImOrderSet<ImList<PropertyDrawViewOrPivotColumn>> getPivotColumns() {
        return pivotColumns.getOrderSet();
    }

    protected NFOrderSet<ImList<PropertyDrawViewOrPivotColumn>> pivotRows = NFFact.orderSet();
    public ImOrderSet<ImList<PropertyDrawViewOrPivotColumn>> getPivotRows() {
        return pivotRows.getOrderSet();
    }

    protected NFOrderSet<PropertyDrawView> pivotMeasures = NFFact.orderSet();
    public ImOrderSet<PropertyDrawView> getPivotMeasures() {
        return pivotMeasures.getOrderSet();
    }

    public ComponentView findById(int id) {
        return mainContainer.findById(id);
    }

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

    public FormView(FormEntity entity, Version version) {
        this.entity = entity;

        mainContainer = containerFactory.createContainer(entity.getDebugPoint());
        mainContainer.main = true;
        setComponentSID(mainContainer, getBoxContainerSID(), version);
        mainContainer.setAddParent(this, (Function<FormView, ContainerView<?>>) fv -> fv.mainContainer);
        mainContainer.setWidth(-3, version);
        mainContainer.setHeight(-3, version);
    }

    public void addFilter(PropertyDrawEntity filterProperty, Version version) {
        PropertyDrawView propertyDrawView = get(filterProperty);
        if (propertyDrawView.filter == null) {
            GroupObjectEntity groupObjectEntity = filterProperty.getNFToDraw(entity, version);
            if (groupObjectEntity.isInTree()) {
                get(groupObjectEntity.treeGroup).addFilter(genID(), propertyDrawView, version);
            } else {
                get(groupObjectEntity).grid.addFilter(genID(), propertyDrawView, version);
            }
        }
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean descending, Version version) {
        defaultOrders.add(get(property), descending, version);
    }

    public void addDefaultOrderFirst(PropertyDrawEntity property, boolean descending, Version version) {
        defaultOrders.addFirst(get(property), descending, version);
    }

    public void addPivotColumn(ImList<PropertyDrawEntityOrPivotColumn> column, Version version) {
        pivotColumns.add(getPropertyDrawViewList(column), version);
    }

    public void addPivotRow(ImList<PropertyDrawEntityOrPivotColumn> row, Version version) {
        pivotRows.add(getPropertyDrawViewList(row), version);
    }

    public void addPivotMeasure(PropertyDrawEntity measure, Version version) {
        pivotMeasures.add(get(measure), version);
    }

    private ImList<PropertyDrawViewOrPivotColumn> getPropertyDrawViewList(ImList<PropertyDrawEntityOrPivotColumn> propertyDrawEntityList) {
        return propertyDrawEntityList.mapListValues(value -> value.getPropertyDrawViewOrPivotColumn(this));
    }

    public PropertyDrawView addPropertyDraw(PropertyDrawEntity property, Version version) {
        PropertyDrawView propertyView = new PropertyDrawView(property, version);
        properties.add(propertyView, ComplexLocation.DEFAULT(), version);
        setComponentSIDs(propertyView, version);
        return propertyView;
    }

    public void movePropertyDraw(PropertyDrawView property, ComplexLocation<PropertyDrawView> location, Version version) {
        properties.add(property, location, version);
    }

    public void updatePropertyDrawContainer(PropertyDrawView property, Version version) {
    }

    private void setComponentSIDs(GridView groupObjectView, Version version) {
        setComponentSIDs((GridPropertyView) groupObjectView, version);
        setComponentSID(groupObjectView.calculations, getCalculationsSID(groupObjectView), version);
    }

    public GroupObjectView addGroupObject(GroupObjectEntity groupObject, Version version) {
        GroupObjectView groupObjectView = new GroupObjectView(genID(), containerFactory, groupObject, version);
        groupObjects.add(groupObjectView, ComplexLocation.DEFAULT(), version);
        if(!groupObjectView.entity.isInTree())
            setComponentSIDs(groupObjectView.grid, version);
        return groupObjectView;
    }

    public void moveGroupObject(GroupObjectView groupObject, ComplexLocation<GroupObjectEntity> location, Version version) {
        groupObjects.add(groupObject, location.map(this::get), version);
    }

    public TreeGroupView addTreeGroup(TreeGroupEntity treeGroup, Version version) {
        TreeGroupView treeGroupView = new TreeGroupView(genID(), containerFactory, treeGroup, version);
        treeGroups.add(treeGroupView, version);
        setComponentSIDs(treeGroupView, version);
        return treeGroupView;
    }

    public void moveTreeGroup(TreeGroupView treeGroup, ComplexLocation<GroupObjectEntity> location, Version version) {
        ImOrderSet<GroupObjectView> treeGroups = treeGroup.groups;
        for(GroupObjectView groupObject : location.isReverseList() ? treeGroups.reverseOrder() : treeGroups)
            groupObjects.add(groupObject, location.map(this::get), version);
    }

    private void setComponentSIDs(GridPropertyView treeGroupView, Version version) {
        setComponentSID(treeGroupView, getGridSID(treeGroupView), version);
        setComponentSID(treeGroupView.toolbarSystem, getToolbarSystemSID(treeGroupView), version);
        setComponentSID(treeGroupView.filtersContainer, getFiltersContainerSID(treeGroupView), version);
        setComponentSID(treeGroupView.filterControls, getFilterControlsComponentSID(treeGroupView), version);
    }

    private void setComponentSIDs(RegularFilterGroupView filterGroupView, Version version) {
        setComponentSID(filterGroupView, getFilterGroupSID(filterGroupView.entity), version);
    }

    private void setComponentSIDs(PropertyDrawView propertyDrawView, Version version) {
        setComponentSID(propertyDrawView, getPropertySID(propertyDrawView.entity), version);
    }

    public RegularFilterGroupView addRegularFilterGroup(RegularFilterGroupEntity filterGroupEntity, Version version) {
        RegularFilterGroupView filterGroupView = new RegularFilterGroupView(filterGroupEntity, version);
        regularFilters.add(filterGroupView, version);
        setComponentSIDs(filterGroupView, version);
        return filterGroupView;
    }
    
    public RegularFilterView addRegularFilter(RegularFilterGroupEntity filterGroup, RegularFilterEntity filter, Version version) {
        RegularFilterGroupView filterGroupView = get(filterGroup);
        return filterGroupView.addFilter(filter, version);
    }


    public ContainerView createContainer(LocalizedString caption, Version version) {
        return createContainer(caption, null, null, containerFactory, version);
    }

    public static ContainerView createContainer(LocalizedString caption, String name, DebugInfo.DebugPoint debugPoint, ContainerFactory<ContainerView> containerFactory, Version version) {
        ContainerView container = containerFactory.createContainer(debugPoint);

        container.setCaption(caption, version);
        container.setName(name, version);

        return container;
    }

    public void addComponentToMapping(ComponentView container, Version version) {
        components.add(container, version);
    }

    public NFOrderSet<ComponentView> components = NFFact.orderSet();

    public ImOrderSet<ComponentView> getComponents() {
        return components.getOrderSet();
    }

    // a group rendered by a CUSTOM REACT container is drawn by React, not the standard grid, so the server must not
    // apply grid-only behavior to its properties (e.g. autoselect, which would turn a foreign-key column's value
    // into a JSON candidate list). A box marked lsf keeps its standard grid, so its group is not React-owned
    public boolean isReactContainerGroup(GroupObjectEntity group) {
        for (ComponentView component : getComponents())
            if (component instanceof ContainerView && ((ContainerView) component).groupObjectBox == group)
                return isReactOwned((ContainerView) component);
        return false;
    }
    private static boolean isReactOwned(ContainerView container) {
        return container.isReact() || getReactContainer(container) != null;
    }

    // the react container that RENDERS this component, null if a standard view is built for it (mirrors
    // GFormController.getReactContainer): a non-lsf child gets no view, so its owner swallows everything below it
    private static ContainerView getReactContainer(ComponentView component) {
        ContainerView parent = component.getContainer();
        if (parent == null)
            return null;
        ContainerView parentOwner = getReactContainer(parent);
        if (parentOwner != null)
            return parentOwner;
        return parent.isReact() && !component.isLsfView() ? parent : null;
    }

    public Iterable<ComponentView> getNFComponentsIt(Version version) {
        return components.getNFIt(version);
    }
    public Iterable<ComponentView> getNFComponentsIt(Version version, boolean allowRead) {
        return components.getNFIt(version, allowRead);
    }

    public ComponentView getComponentBySID(String sid, Version version) {
        for(ComponentView component : components.getNFListIt(version))
            if(sid.equals(component.getSID()))
                return component;

        return null;
    }

    public ComponentView getComponentBySID(String sid) {
        for(ComponentView component : components.getListIt())
            if(sid.equals(component.getSID()))
                return component;

        return null;
    }

    public ContainerView getMainContainer() {
        return mainContainer;
    }

    public GroupObjectView getGroupObject(GroupObjectEntity entity) {
        if (entity == null) {
            return null;
        }
        for (GroupObjectView groupObject : getGroupObjectsIt())
            if (entity.equals(groupObject.entity))
                return groupObject;
        return null;
    }

    public ObjectView getObject(ObjectEntity entity) {
        if (entity == null) {
            return null;
        }
        for (GroupObjectView groupObject : getGroupObjectsIt())
            for(ObjectView object : groupObject.objects)
                if (entity.equals(object.entity))
                    return object;
        return null;
    }

    public TreeGroupView getTreeGroup(TreeGroupEntity entity) {
        if (entity == null) {
            return null;
        }
        for (TreeGroupView treeGroup : getTreeGroupsIt())
            if (entity.equals(treeGroup.entity))
                return treeGroup;
        return null;
    }

    public PropertyDrawView getProperty(PropertyDrawEntity entity) {
        if (entity == null) {
            return null;
        }
        for (PropertyDrawView property : getPropertiesIt()) {
            if (entity.equals(property.entity)) {
                return property;
            }
        }
        return null;
    }

    public PropertyDrawView getNFProperty(PropertyDrawEntity entity, Version version) {
        if (entity == null) {
            return null;
        }
        for (PropertyDrawView property : getNFPropertiesIt(version)) {
            if (entity.equals(property.entity)) {
                return property;
            }
        }
        return null;
    }

    public List<PropertyDrawView> getProperties(GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<>();

        for (PropertyDrawView property : getPropertiesIt()) {
            if (groupObject.equals(property.entity.getToDraw(entity))) {
                result.add(property);
            }
        }

        return result;
    }

    public void setBackground(PropertyDrawView property, Color background, Version version) {
        property.setBackground(background, version);
    }

    public void setChangeKey(PropertyDrawView property, KeyStroke keyStroke, Version version) {
        property.setChangeKey(new InputBindingEvent(keyStroke != null ? new KeyInputEvent(keyStroke) : null, null), version);
    }
    public void setChangeMouse(PropertyDrawView property, String mouseStroke, Version version) {
        property.setChangeMouse(new InputBindingEvent(new MouseInputEvent(mouseStroke), null), version);
    }

    public void setComponentSID(ContainerView container, String sid, Version version) {
        setComponentSID((ComponentView) container, sid, version);
    }
    protected void setComponentSID(BaseComponentView component, String sid, Version version) {
        setComponentSID((ComponentView) component, sid, version);
    }
    public void setComponentSID(ComponentView component, String sid, Version version) {
        component.setSID(sid);
        addComponentToMapping(component, version);
    }

    public ContainerView getContainerBySID(String sid, Version version) {
        ComponentView component = getComponentBySID(sid, version);
        if (component != null && !(component instanceof ContainerView)) {
            throw new IllegalStateException(sid + " component has to be container");
        }
        return (ContainerView) component;
    }

    public ContainerView getContainerBySID(String sid) {
        ComponentView component = getComponentBySID(sid);
        if (component != null && !(component instanceof ContainerView)) {
            throw new IllegalStateException(sid + " component has to be container");
        }
        return (ContainerView) component;
    }

    private static String getBoxContainerSID() {
        return FormContainerSet.BOX_CONTAINER;
    }

    private static String getFilterGroupSID(RegularFilterGroupEntity entity) {
        return FILTERGROUP_COMPONENT + "(" + entity.getSID() + ")";
    }

    private static String getPropertySID(PropertyDrawEntity entity) {
        return PROPERTY_COMPONENT + "(" + entity.getSID() + ")";
    }

    private static String getGridSID(PropertyGroupContainersView entity) {
        return GRID_COMPONENT + "(" + entity.getPropertyGroupContainerSID() + ")";
    }

    private static String getToolbarSystemSID(PropertyGroupContainersView entity) {
        return TOOLBAR_SYSTEM_COMPONENT + "(" + entity.getPropertyGroupContainerSID() + ")";
    }

    private static String getFiltersContainerSID(PropertyGroupContainersView entity) {
        return FILTERS_CONTAINER + "(" + entity.getPropertyGroupContainerSID() + ")";
    }
    
    private static String getFilterControlsComponentSID(PropertyGroupContainersView entity) {
        return FILTER_CONTROLS_COMPONENT + "(" + entity.getPropertyGroupContainerSID() + ")";
    }
    
    private static String getCalculationsSID(PropertyGroupContainersView entity) {
        return entity.getPropertyGroupContainerSID() + ".calculations";
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.serializeObject(outStream, mainContainer);
        pool.serializeCollection(outStream, getTreeGroups());
        pool.serializeCollection(outStream, getGroupObjectsListIt());
        pool.serializeCollection(outStream, getPropertiesList());
        pool.serializeCollection(outStream, getRegularFiltersList());

        ImOrderMap<PropertyDrawView, Boolean> defaultOrders = getDefaultOrders();
        int size = defaultOrders.size();
        outStream.writeInt(size);
        for (int i=0;i<size;i++) {
            pool.serializeObject(outStream, defaultOrders.getKey(i));
            outStream.writeBoolean(defaultOrders.getValue(i));
        }

        ImOrderSet<ImList<PropertyDrawViewOrPivotColumn>> pivotColumns = getPivotColumns();
        ImOrderSet<ImList<PropertyDrawViewOrPivotColumn>> pivotRows = getPivotRows();
        for(GroupObjectView groupObject : getGroupObjectsIt()) {
            if(!hasPivotColumn(pivotColumns, pivotRows, groupObject.getSID())) {
                pivotColumns = SetFact.<ImList<PropertyDrawViewOrPivotColumn>>singletonOrder(ListFact.singleton(new PivotColumn(groupObject.entity))).addOrderExcl(pivotColumns);
            }
        }

        serializePivot(pool, outStream, pivotColumns);
        serializePivot(pool, outStream, pivotRows);
        pool.serializeCollection(outStream, getPivotMeasures());

        pool.writeString(outStream, entity.getSID());
        pool.writeString(outStream, entity.getCreationPath());
        pool.writeString(outStream, entity.getPath());
        pool.writeInt(outStream, overridePageWidth);
        serializeAsyncExecMap(outStream, entity.getAsyncExecMap(pool.context), pool.context);
    }

    private boolean hasPivotColumn(ImOrderSet<ImList<PropertyDrawViewOrPivotColumn>> pivotColumns, ImOrderSet<ImList<PropertyDrawViewOrPivotColumn>> pivotRows, String groupObject) {
        for(ImList<PropertyDrawViewOrPivotColumn> propertyList : pivotColumns) {
            for (PropertyDrawViewOrPivotColumn property : propertyList) {
                if(property instanceof PivotColumn && ((PivotColumn) property).groupObject.equals(groupObject))
                    return true;
            }
        }
        for(ImList<PropertyDrawViewOrPivotColumn> propertyList : pivotRows) {
            for (PropertyDrawViewOrPivotColumn property : propertyList) {
                if(property instanceof PivotColumn && ((PivotColumn) property).groupObject.equals(groupObject))
                    return true;
            }
        }
        return false;
    }

    private void serializePivot(ServerSerializationPool pool, DataOutputStream outStream, ImOrderSet<ImList<PropertyDrawViewOrPivotColumn>> list) throws IOException {
        int pivotColumnsSize = list.size();
        outStream.writeInt(pivotColumnsSize);
        for(ImList<PropertyDrawViewOrPivotColumn> pivotColumn : list) {
            pool.serializeCollection(outStream, pivotColumn);
        }
    }

    private void serializeAsyncExecMap(DataOutputStream outStream, Map<FormEvent, AsyncEventExec> asyncExecMap, FormInstanceContext context) throws IOException {
        outStream.writeInt(asyncExecMap.size());
        for(Map.Entry<FormEvent, AsyncEventExec> entry : asyncExecMap.entrySet()) {
            entry.getKey().serialize(outStream);
            AsyncSerializer.serializeEventExec(entry.getValue(), context, outStream);
        }
    }

    public void finalizeAroundInit() {
        treeGroups.finalizeChanges();
        groupObjects.finalizeChanges();
        
        for(TreeGroupView property : getTreeGroupsIt())
            property.finalizeAroundInit();

        for(GroupObjectView property : getGroupObjectsIt())
            property.finalizeAroundInit();

        for(PropertyDrawView property : getPropertiesIt())
            property.finalizeAroundInit();

        defaultOrders.finalizeChanges();

        mainContainer.finalizeAroundInit();

        for(RegularFilterGroupView regularFilter : getRegularFiltersIt())
            regularFilter.finalizeAroundInit();

        for(ComponentView component : getComponents())
            component.finalizeAroundInit();

        pivotColumns.finalizeChanges();
        pivotRows.finalizeChanges();
        pivotMeasures.finalizeChanges();

        components.finalizeChanges();

        checkLsfViews();
        checkReactProjectionNames();
    }

    // A CUSTOM REACT container projects its groups and properties into a JS `data` object (see the web client's
    // GReactFormData), which owns some names at every level. A projected integration SID that takes one of them, or that
    // two projected items share, would silently overwrite the other — so reject it here, with the rest of the design
    // checks: the whole form is then rejected once, when it is built, instead of failing in every browser that opens it.
    // The names below mirror what GReactFormData writes; keep them in sync with it.
    private void checkReactProjectionNames() {
        // There is no `meta` object: everything sits directly in one flat namespace at each level, so a projected name
        // that takes an infrastructure key, or that two projected things share, silently overwrites. Reject it here.
        Map<ContainerView, Set<String>> topNames = new HashMap<>();     // data.*           : groups + form-level props + containers
        Map<GroupObjectEntity, Set<String>> nodeNames = new HashMap<>(); // data.<group>.*    : list/byKey/keys/count/options + column & panel props
        Map<GroupObjectEntity, Set<String>> rowNames = new HashMap<>();  // data.<group>.list[i].* : key/isCurrent/objects/background/foreground/selected + cell props

        for (ComponentView component : getComponents()) {
            if (component instanceof ContainerView) {
                // a group box is projected as data.<groupSID> on the scope it is rendered by
                GroupObjectEntity group = ((ContainerView) component).groupObjectBox;
                ContainerView scope = group != null ? getOwningReactContainer(component) : null;
                if (scope != null)
                    claimProjectionName(topNames, scope, group.getSID(), "object group '" + group.getSID() + "'");

                // a container the author DECLARED (`NEW <name>` in DESIGN) is data.<componentSID> = {caption, image},
                // directly in data beside the groups and the form-level props (`{}` when it has neither). The generated
                // boxes of a group are not projected and take no name here.
                ContainerView container = (ContainerView) component;
                ContainerView descriptorScope = getProjectedContainerScope(container);
                if (descriptorScope != null)
                    claimProjectionName(topNames, descriptorScope, container.getSID(), "container '" + container.getSID() + "'");
            }
        }
        for (PropertyDrawView property : getPropertiesIt()) {
            String integrationSID = property.entity.getIntegrationSID();
            if (integrationSID == null)
                continue;
            boolean lsf = property.isLsfView(); // the platform draws its value: not projected, only its caption/image are
            GroupObjectEntity group = property.entity.getToDraw(entity);
            if (group == null) { // a form-level property: one object at data.<integrationSID>
                ContainerView scope = lsf ? property.getContainer() // an LSF draw's parent IS react (checkLsfViews ran first)
                        : getOwningReactContainer(property);
                if (scope != null)
                    claimProjectionName(topNames, scope, integrationSID, "form property '" + integrationSID + "'");
            } else if (isReactContainerGroup(group) && property.entity.getColumnGroupObjects().isEmpty()) {
                // grouped-in-columns draws are not projected (buildGroupEntry skips them); everything else is an object keyed by
                // its integration sid at the NODE (a list column's caption, or a panel value) and, for a react-owned list
                // cell, at the ROW too
                String source = "property '" + group.getSID() + "." + integrationSID + "'";
                claimProjectionName(nodeNames, group, integrationSID, source, "list", "byKey", "keys", "count", "options");
                if (property.entity.isList(entity) && !lsf) // an LSF list property has no per-row cell, only its column
                    claimProjectionName(rowNames, group, integrationSID, source, "key", "isCurrent", "objects", "background", "foreground", "selected");
            }
        }
    }

    // the scope whose data carries this container's entry, or null when it has none: only a container the author
    // DECLARED is projected, and an LSF-VIEW one is placed by the scope it sits in rather than by the scope that owns
    // it - the ownership walk stops at an lsf child. The client's statement of the same rule bears the same name
    // (GReactFormData.getProjectedContainerScope); this one reserves the names that one emits.
    private ContainerView getProjectedContainerScope(ContainerView container) {
        if (container.isLsfView()) // an lsf child's parent IS a react container - checkLsfViews, which runs first, rejected anything else
            return container.getContainer();
        return container.declared ? getOwningReactContainer(container) : null; // declared OR lsf, nothing for the rest
    }


    private <K> void claimProjectionName(Map<K, Set<String>> names, K owner, String name, String source, String... reserved) {
        for (String reservedName : reserved)
            if (reservedName.equals(name))
                throw new IllegalStateException(formErrorPrefix() + "cannot project " + source + " into the react component: '" + name
                        + "' is a reserved name at this data level. Give the property an explicit EXTID");
        if (!names.computeIfAbsent(owner, k -> new HashSet<>()).add(name))
            throw new IllegalStateException(formErrorPrefix() + "cannot project " + source + " into the react component: '" + name
                    + "' is already projected at this data level. Give one of them an explicit EXTID");
    }

    // this throws while the logics is being built, so the message is all the developer gets: name the form
    private String formErrorPrefix() {
        return entity.getCreationPath() + " form '" + entity.getSID() + "': ";
    }

    // the react container that OWNS this component: the one that renders it, or the component itself when it is the
    // react container (mirrors GFormController.getOwningReactContainer)
    private static ContainerView getOwningReactContainer(ComponentView component) {
        ContainerView outer = getReactContainer(component);
        if (outer != null)
            return outer;
        return component instanceof ContainerView && ((ContainerView) component).isReact() ? (ContainerView) component : null;
    }

    // the design is complete here, so `lsf` can finally be checked against the container tree
    private void checkLsfViews() {
        for (ComponentView component : getComponents()) // the property draws are components too, so this covers them
            checkLsfView(component);
    }

    private void checkLsfView(ComponentView component) {
        if (!component.isLsfView())
            return;

        ContainerView container = component.getContainer();
        if (container == null || !container.isReact())
            throw new IllegalStateException("lsf is set for '" + component.getSID() + "', which is not a child of a CUSTOM REACT container");

        // a react container that is itself a non-lsf child of a react container is SWALLOWED - the outer component
        // draws it from data and it never gets a view of its own - so nothing could place this child's view either
        ContainerView owner = getReactContainer(container);
        if (owner != null)
            throw new IllegalStateException("lsf is set for '" + component.getSID() + "', but its container '" + container.getSID()
                    + "' is itself rendered by the react component '" + owner.getCustom()
                    + "' — nothing would place it; set lsf on '" + container.getSID() + "' as well to give it its own view");

        if (component instanceof PropertyDrawView) {
            PropertyDrawEntity<?, ?> property = ((PropertyDrawView) component).entity;
            GroupObjectEntity toDraw = property.getToDraw(entity); // the no-arg form is unset when the group comes from the property context
            boolean reactGroup = toDraw != null && isReactContainerGroup(toDraw);

            if (property.isList(entity)) {
                // a GRID property is drawn once per ROW and the react component places each of those, so its group
                // being rendered by that component is exactly what makes this work
                if (!reactGroup)
                    throw new IllegalStateException("LSF is set for property '" + component.getSID()
                            + "', whose object group is not rendered by a CUSTOM REACT container — nothing would place the per-row renderers");

                // per-row rendering rests on the renderer key being the ROW key. With column groups the full key joins
                // row and column, so a value would be written under the joined key and read under the row key, and
                // every edit would be lost without a word
                if (!property.getColumnGroupObjects().isEmpty())
                    throw new IllegalStateException("LSF is set for property '" + component.getSID()
                            + "', which is grouped in columns — it draws one editor per ROW and cannot address a row-and-column cell");
            } else if (reactGroup)
                // a PANEL property is drawn once, for its group's current object - and a group the react component
                // renders has no client controller, so no view would ever be built and there would be nothing to place
                throw new IllegalStateException("lsf is set for property '" + component.getSID() + "', whose object group is rendered by the react component '"
                        + container.getCustom() + "' — set lsf on the group's box instead");
        }
    }

    public final ContainerFactory<ContainerView> containerFactory = debugPoint -> new ContainerView(genID(), debugPoint);

    public void prereadAutoIcons(ConnectionContext context) {
        mainContainer.prereadAutoIcons(this, context);
    }

    // the problem is that if removed components are not put somewhere they are not finalized
    public void removeComponent(ComponentView component, Version version) {
        if(component instanceof PropertyDrawView) {
            ((PropertyDrawView) component).entity.setRemove(true, version);
        }
        component.removeFromParent(version);
    }

    public FormView(This src, ObjectMapping mapping) {
        super(src, mapping);

        entity = mapping.get(src.entity);
        mainContainer = mapping.get(src.mainContainer);
    }
    // no extend

    public void add(This src, ObjectMapping mapping) {
        mapping.add(treeGroups, src.treeGroups);
        mapping.add(groupObjects, src.groupObjects);
        mapping.add(properties, src.properties);
        mapping.add(regularFilters, src.regularFilters);

        mapping.add(defaultOrders, src.defaultOrders);

        mapping.addl(pivotColumns, src.pivotColumns);

        mapping.addl(pivotRows, src.pivotRows);

        mapping.add(pivotMeasures, src.pivotMeasures);

        mapping.add(components, src.components);
    }

    @Override
    public FormEntity getAddParent(ObjectMapping mapping) {
        return entity;
    }
    @Override
    public This getAddChild(FormEntity formEntity, ObjectMapping mapping) {
        return (This) formEntity.view;
    }
    @Override
    public This copy(ObjectMapping mapping) {
        return (This)new FormView<>((This)this, mapping);
    }
}