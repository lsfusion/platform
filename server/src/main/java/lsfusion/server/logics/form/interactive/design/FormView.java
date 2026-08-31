package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.event.*;
import lsfusion.server.base.Custom;
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
        return partScope(getGroupDrawComponent(group)) != null; // where its ROWS are drawn
    }

    // the component that actually DRAWS a group: the tree it belongs to, or its own grid. This is the same component
    // the client asks about (GFormController.isReactOwned(GGroupObject): group.grid, else group.parent), so the two
    // agree by construction. Asking the group's BOX instead does not work for a tree - one box draws all of the tree's
    // groups, so groupObjectBox names none of them - and it answers for the box rather than for what draws the group,
    // which differ once a design MOVEs the grid out of its box or marks the grid lsf.
    private ComponentView getGroupDrawComponent(GroupObjectEntity group) {
        if (group.isInTree()) // a group in a tree has no grid of its own (GroupObjectView builds one only outside a tree)
            return get(group.treeGroup);
        return get(group).grid;
    }


    // whether any react view sees this group at all (mirrors GReactFormData.isProjectedGroup)
    private boolean isProjectedGroup(GroupObjectEntity group) {
        return !getGroupScopes(group).isEmpty();
    }

    // the scopes a group's node appears in - every container that draws a PART of it (mirrors
    // GReactFormData.getGroupScopes, and must gain a kind at the same time that one does)
    private List<ContainerView> getGroupScopes(GroupObjectEntity group) {
        List<ContainerView> scopes = new ArrayList<>();
        for (ComponentView producer : getPartProducers(group))
            addGroupScope(scopes, partScope(producer));
        return scopes;
    }

    // THE list of components that produce a part of a group - the one place a KIND is enumerated on this side, and
    // the twin of GReactFormData.getPartProducers: a branch that gives a component a part adds it to BOTH in one
    // commit, or the two sides answer differently. Two kinds today: the component that draws the rows, and each
    // panel draw. What is deliberately absent is the chrome (toolbar, filters, calculations) - it draws nothing yet.
    private List<ComponentView> getPartProducers(GroupObjectEntity group) {
        List<ComponentView> producers = new ArrayList<>();
        producers.add(getGroupDrawComponent(group));
        for (PropertyDrawView property : getPropertiesIt())
            if (!property.entity.isList(entity) && group.equals(property.entity.getToDraw(entity)))
                producers.add(property);
        return producers;
    }

    // ... and what those producers WRITE on the node in this scope, which is what a projected name can collide with.
    // Accumulated over the producers actually placed here rather than inferred from "is the grid in this scope",
    // so a kind that lands in a container of its own brings its names with it instead of needing a test beside this.
    private String[] getReservedNodeNames(GroupObjectEntity group, ContainerView scope) {
        String[] reserved = NODE_OWN_NAMES;
        if (partScope(getGroupDrawComponent(group)) == scope)
            reserved = concat(reserved, GRID_PART_NAMES);
        return reserved;
    }
    private static void addGroupScope(List<ContainerView> scopes, ContainerView scope) {
        if (scope != null && !scopes.contains(scope))
            scopes.add(scope);
    }

    // (mirrors GReactFormData.partScope)
    private ContainerView partScope(ComponentView component) {
        return getOwningReactContainer(component);
    }

    // ... and WHERE ITS DESCRIPTOR GOES - the container that labels the boundary it places
    // (mirrors GReactFormData.descriptorScope)
    private ContainerView descriptorScope(ComponentView component) {
        return component != null && component.isLsfView() ? component.getContainer() : partScope(component);
    }

    // the react container that RENDERS this component, null if a standard view is built for it (mirrors
    // GComponent.getDrawingReactContainer): a non-lsf child gets no view, so its owner swallows everything below it
    private static ContainerView getReactContainer(ComponentView component) {
        if (component == null)
            return null;
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

        // what draws a container comes first: every check below reads it - whether a child may carry `lsf`, whether a
        // name is projected, what a place may name - so a container whose custom is not a renderer at all would fail
        // one of them instead, naming a consequence rather than the mistake
        checkCustomValue();
        checkCustomTabbed();
        checkCustomReactProperty();
        checkCustomReactSwallowed();
        checkCustomTemplatePlaces();

        checkLsfViews();
        checkReactProjectionNames();
    }

    // the same three vocabularies a window's CUSTOM is read by, plus the container's own '': a component name, markup,
    // or nothing to lay the children out in order. Anything else is a mistake and not a fourth kind - without this a
    // misspelled component name is markup that happens to contain no tag, and the container draws that name as its text
    private void checkCustomValue() {
        for (ComponentView component : getComponents())
            if (component instanceof ContainerView) {
                ContainerView<?> container = (ContainerView<?>) component;
                String custom = container.getCustom();
                if (custom != null && !custom.isEmpty() && !Custom.isReactComponent(custom) && !Custom.isHtmlTemplate(custom))
                    throw new IllegalStateException(formErrorPrefix() + "custom '" + custom + "' of container '" + container.getSID()
                            + "' is neither a React component name (an uppercase letter followed by letters, digits, '_' or '$')"
                            + " nor an HTML template (it must contain a tag)");

                // said here rather than let the client meet it: a written value longer than one modified-UTF string is
                // not refused when the form is serialized but thrown, and the form then refuses to open at all
                if (Custom.isTooLong(custom))
                    throw new IllegalStateException(formErrorPrefix() + "custom of container '" + container.getSID() + "' is "
                            + custom.length() + " characters long, which is more than the " + Custom.MAX_LENGTH
                            + " a written value can carry; a template this size is computed by a property instead");
            }
    }

    // a container draws its children ONE way, and tabbed is one of them: with both, whichever the client happens to
    // test for first wins and the other is silently ignored - and a tabbed container handed a custom PROPERTY has
    // nowhere to put the recomputed template at all
    private void checkCustomTabbed() {
        for (ComponentView component : getComponents())
            if (component instanceof ContainerView) {
                ContainerView<?> container = (ContainerView<?>) component;
                if (container.isTabbed() && (container.getCustom() != null || container.getPropertyCustom() != null))
                    throw new IllegalStateException(formErrorPrefix() + "container '" + container.getSID() + "' is both tabbed"
                            + " and drawn by custom; a container is drawn one way, so only one of the two can be given");
            }
    }

    // a literal `custom` beside a `custom` property is the pair the rest of the design uses (caption / propertyCaption):
    // the literal is what the container starts with, the property recomputes it. The one combination that is not a pair
    // is a React component, which draws the container itself and would never be handed a computed template
    // a react container that is itself a non-lsf child of a react container is SWALLOWED: GFormLayout.addContainers
    // skips the whole projected subtree, so no view is built for it, its `custom` is never read, and the outer
    // component is left to draw it from `data`. Its component is therefore drawn by nobody, silently - the mirror
    // image of the case checkLsfView already refuses, and until now the only one of the two that said nothing
    private void checkCustomReactSwallowed() {
        for (ComponentView component : getComponents())
            if (component instanceof ContainerView) {
                ContainerView container = (ContainerView) component;
                ContainerView owner = container.isReact() ? getReactContainer(container) : null; // climbs, so a plain box in between does not hide it
                if (owner != null)
                    throw new IllegalStateException(formErrorPrefix() + "container '" + container.getSID() + "' is the React"
                            + " component '" + container.getCustom() + "', but it is itself drawn by the React component '"
                            + owner.getCustom() + "', which draws its children from data - so nothing would ever build it;"
                            + " set lsf = TRUE on '" + container.getSID() + "' to give it a view of its own and place it"
                            + " with <Lsf name=\"" + container.getSID() + "\"/>");
            }
    }

    private void checkCustomReactProperty() {
        for (ComponentView component : getComponents())
            if (component instanceof ContainerView) {
                ContainerView container = (ContainerView) component;
                if (container.getPropertyCustom() != null && container.isReact())
                    throw new IllegalStateException(formErrorPrefix() + "custom of container '" + container.getSID()
                            + "' is both the React component '" + container.getCustom() + "' and a property; a component draws"
                            + " the container itself, so a computed template would never be drawn");
            }
    }

    // a child drawn inline gives a place to each of its parts - sID.caption, sID.comment - so a name belongs to a
    // component when it is its sID or names a part below it. Only the sID: a property is named as the design names it,
    // PROPERTY(qty), and not by the property alone - one name, one spelling, and no question of which child answers
    // when a container and a property could both be read as one
    private static boolean names(String name, String sid) {
        return namesOrPart(name, sid);
    }

    private static boolean namesOrPart(String name, String base) {
        return name.equals(base) || name.startsWith(base + ".");
    }

    // A container drawn by an HTML template gives each child it shows a <Lsf:sID> place. A place naming nothing the form
    // has at all would never be filled and nothing would say so, so the form is rejected here instead - once, when it is
    // built, with the rest of the design checks. A name the form HAS but this container no longer holds is left alone: an
    // extending module may move a child out, and an unfilled place is what the client does with that anyway.
    // mapPlaces is the traversal here, not a rewrite - the check is the throw, and its result is discarded
    private void checkCustomTemplatePlaces() {
        for (ComponentView component : getComponents()) {
            if (!(component instanceof ContainerView))
                continue;

            ContainerView<?> container = (ContainerView<?>) component;
            String custom = container.getCustom();
            if (custom == null || Custom.isReactComponent(custom))
                continue;

            try {
                Custom.mapPlaces(custom, name -> {
                    for (ComponentView anywhere : getComponents()) // getComponents holds every child too
                        if (names(name, anywhere.getSID()))
                            return name;

                    throw new Custom.PlaceError("'" + name + "' is not a component of this form");
                });
            } catch (Custom.PlaceError e) {
                throw new IllegalStateException(formErrorPrefix() + "in the template of container '" + container.getSID() + "': " + e.getMessage());
            }
        }
    }

    // A CUSTOM REACT container projects its groups and properties into a JS `data` object (see the web client's
    // GReactFormData), which owns some names at every level. A projected integration SID that takes one of them, or that
    // two projected items share, would silently overwrite the other — so reject it here, with the rest of the design
    // checks: the whole form is then rejected once, when it is built, instead of failing in every browser that opens it.
    // The names below mirror what GReactFormData writes; keep them in sync with it.
    private void checkReactProjectionNames() {
        // data.* : groups + form-level props + containers - and, since a group and a form-level property are named on
        // that container's CONTROLLER too (controller.<name>), one claim per scope says both. Their reserved list is
        // the controller's, which contains the projection's; a container's descriptor is data-only and takes the shorter
        Map<ContainerView, Set<String>> topNames = new HashMap<>();
        // keyed by (scope, group), because that is what a node is: the same group has a different node in each
        // container that draws a part of it, and a name collides only with what THAT node carries
        Map<Pair<ContainerView, GroupObjectEntity>, Set<String>> nodeNames = new HashMap<>(); // data.<group>.*
        Map<Pair<ContainerView, GroupObjectEntity>, Set<String>> rowNames = new HashMap<>();  // data.<group>.list[i].*

        // a group is projected as data.<groupSID> on every scope that sees it - asked of the group, not of a box, so
        // the groups of a tree (which share one box, named by none of them) are claimed too
        for (GroupObjectView groupObject : getGroupObjectsIt()) {
            GroupObjectEntity group = groupObject.entity;
            List<ContainerView> scopes = getGroupScopes(group);
            if (!scopes.isEmpty())
                checkProjectedGroupSID(group);
            for (ContainerView scope : scopes)
                claimProjectionName(topNames, scope, group.getSID(), "object group '" + group.getSID() + "'", CONTROLLER_NAMES);
        }

        for (ComponentView component : getComponents()) {
            ContainerView descriptorScope = getProjectedContainerScope(component);
            if (descriptorScope != null) // named by its kind, since that is what the author sees in the error
                claimProjectionName(topNames, descriptorScope, component.getSID(),
                        (component instanceof ContainerView ? "container '" : "component '") + component.getSID() + "'", TOP_NAMES);
        }
        for (PropertyDrawView property : getPropertiesIt()) {
            String integrationSID = property.entity.getIntegrationSID();
            if (integrationSID == null)
                continue;
            boolean lsf = property.isLsfView(); // the platform draws its value: not projected, only its caption/image are
            GroupObjectEntity group = property.entity.getToDraw(entity);
            boolean columns = !property.entity.getColumnGroupObjects().isEmpty();
            if (group == null) { // a form-level property: one object at data.<integrationSID>
                ContainerView scope = descriptorScope(property); // an LSF draw's entry is its descriptor, in its own container
                if (scope == null) // nothing projects it, so nothing here has to be able to name it
                    continue;
                checkProjectedDraw(columns, integrationSID, "form property");
                claimProjectionName(topNames, scope, integrationSID, "form property '" + integrationSID + "'", CONTROLLER_NAMES);
            } else if (isProjectedGroup(group)) {
                boolean list = property.entity.isList(entity);
                // a name is claimed WHERE ITS ENTRY LANDS, which is one container: a list draw's column entry where
                // the grid's part is, a panel draw's entry where the draw itself is. An LSF PANEL draw lands nowhere
                // on the node at all - its descriptor is a top-level entry, claimed with the components above.
                ContainerView scope = list ? partScope(getGroupDrawComponent(group)) : (lsf ? null : partScope(property));
                // ... and the shape refusals are asked of THE DRAW, not of its group: a group is projected when ANY of
                // its components is, so a draw that lands in a classic grid nobody projects is nobody's business here -
                // no projection carries it and no name can reach it. An lsf draw has no scope on the node by
                // construction and still carries a descriptor, so it is asked by that instead
                if (scope != null || getProjectedContainerScope(property) != null)
                    checkProjectedDraw(columns, integrationSID, "property of object group '" + group.getSID() + "'");
                if (scope == null)
                    continue;
                String source = "property '" + group.getSID() + "." + integrationSID + "'";
                // ... and it collides with the names that SCOPE carries, not with the ones another container does: a
                // container holding one panel property of the group has no `list` for a property called `list` to take,
                // while a panel property MOVEd in beside the grid does meet it
                claimProjectionName(nodeNames, Pair.create(scope, group), integrationSID, source,
                        getReservedNodeNames(group, scope));
                if (list && !lsf) // an LSF list property has no per-row cell, only its column
                    claimProjectionName(rowNames, Pair.create(scope, group), integrationSID, source, ROW_NAMES);
            }
        }

    }

    // the scope whose data carries this component's DESCRIPTOR entry, or null when it has none: a container the author
    // DECLARED, and every `lsf` component whatever kind it is - the platform draws it and React only labels the
    // boundary it places. Not an `lsf` LIST draw: React draws its group, so its descriptor is the column entry on the
    // group's node, claimed with the other property names. An LSF component is placed by the scope it SITS in rather
    // than by the scope that owns it - the ownership walk stops at an lsf child. The client's statement of the same
    // rule is GReactFormData.isProjectedContainer/getProjectedContainerScope; this one reserves the names that emits.
    private ContainerView getProjectedContainerScope(ComponentView component) {
        if (component.isLsfView())
            return component instanceof PropertyDrawView && ((PropertyDrawView) component).entity.isList(entity)
                    ? null : descriptorScope(component);
        return component instanceof ContainerView && ((ContainerView<?>) component).declared ? descriptorScope(component) : null;
    }


    // the controller carries the same names the projection does - a group is controller.<groupSID> exactly as it is
    // data.<groupSID> - plus what only it has: the members every level answers to. A name is claimed at BOTH ends here,
    // so a collision is a form that does not build rather than an accessor that is silently missing at run time (the
    // controller can only skip a colliding member, and there would be nothing left to address the group with).
    // `__proto__` is not a name JS lets an object have: `obj["__proto__"] = v` calls the legacy prototype SETTER, so
    // the entry is not written and the object's prototype is replaced instead - the projection would silently lose the
    // property and hand the view an object whose members come from somewhere else. Refused at every level it could be
    // written at, like any other name this surface owns
    private static final String PROTO = "__proto__";
    private static final String[] TOP_NAMES = {PROTO};
    // what a controller carries beside its members: the four verbs every controller has (GController.extendController)
    // and the batch that is the members' shortcut. A projected name equal to one of them would have to be dropped,
    // and the verbs are how everything outside this surface is reached
    private static final String[] CONTROLLER_NAMES = {"exec", "eval", "evalAction", "change", "properties", PROTO};
    // what a group's node carries besides the properties, ONE LINE PER PRODUCER: a branch that gives a component a
    // part of its own adds its line here rather than editing another's, so two of them merge instead of colliding.
    // A name in this array is claimed for the node's DATA and for the group's controller MEMBERS alike - `change` is
    // the group's own verb - so a feature that writes on the node has one array to update, not two.
    private static final String[] GRID_PART_NAMES = {
            "list", "byKey", "keys", "options",                 // the grid's part
    };
    private static final String[] NODE_OWN_NAMES = {
            "properties", "change", PROTO, "__groupSID",        // the node's own: its index, its verb, and the stamp
    };                     // __groupSID is written by GReactFormData.setGroupSID - non-enumerable, and non-writable, so
                           // a property taking that name would either vanish from the node or throw when it is written
    private static final String[] ROW_NAMES = {
            "key", "isCurrent", "objects", "background", "foreground", "selected", PROTO, // the grid's part: a ROW
    };

    // a property GROUPED IN COLUMNS is one cell per row AND column, and every name a react view has - in `data`, on
    // the controller - means one ROW, so such a cell is nothing a name can reach. Where the projection CARRIES the
    // name, that is not a runtime surprise to report but a form that cannot mean what it says, so it is refused here.
    // Only there: a columns draw on a group no react container sees is nobody's business - no projection carries it,
    // so no controller names it, and the classic surface that still can (its own stringly-typed changeProperty, #1655)
    // refuses it when the call is made. What this DOES settle is what the projection itself hands out: inside it a
    // name means one ordinary draw, and a react view's own state reads those names without asking again.
    // a group of SEVERAL objects that the author did not name has no SID of its own: it is synthesized from the object
    // names joined with dots (`OBJECTS (d = X, t = Y)` is `d.t`). That is a legal form everywhere else, but here the
    // name would be `data['d.t']` on one side and `controller['d.t'].name` on the other, and every qualified name a
    // view writes would carry two dots. A projected group is asked to have a name of its own instead - `OBJECTS
    // pair = (d = X, t = Y)` - which the form language already allows.
    private void checkProjectedGroupSID(GroupObjectEntity group) {
        if (group.getSID().indexOf('.') >= 0)
            throw new IllegalStateException(formErrorPrefix() + "cannot project object group '" + group.getSID()
                    + "': it is a group of several objects with no name of its own, so its SID is their names joined"
                    + " with dots - and a react view names a group as one word. Name the group: OBJECTS <name> = ("
                    + group.getSID().replace('.', ',') + ")");
    }

    private void checkProjectedDraw(boolean columns, String integrationSID, String source) {
        // a qualified name on the controller - `<groupSID>.<integrationSID>` - splits at its LAST dot, which is what
        // makes a group of SEVERAL objects addressable: its own SID is all of them joined with dots. That reading
        // rests on the PROPERTY half carrying none, and an EXTID is an arbitrary string, so a dot in one would be
        // read as a group prefix and the name would ask for a group that does not exist
        if (integrationSID.indexOf('.') >= 0)
            throw new IllegalStateException(formErrorPrefix() + "cannot project " + source + " '" + integrationSID
                    + "': its integration name carries a dot, and a name is read as '<object group>.<property>' -"
                    + " everything before the LAST dot is the group. Give it an EXTID without a dot");
        if (columns)
            throw new IllegalStateException(formErrorPrefix() + "cannot project " + source + " '" + integrationSID
                    + "': it is grouped in COLUMNS - one cell per row AND column - and a react view names one ROW."
                    + " Draw it as an ordinary property, or keep it out of what a react container projects");
    }

    private static String[] concat(String[] first, String[] second) {
        String[] both = new String[first.length + second.length];
        System.arraycopy(first, 0, both, 0, first.length);
        System.arraycopy(second, 0, both, first.length, second.length);
        return both;
    }

    private <K> void claimProjectionName(Map<K, Set<String>> names, K owner, String name, String source, String... reserved) {
        for (String reservedName : reserved)
            if (reservedName.equals(name))
                throw new IllegalStateException(formErrorPrefix() + "cannot project " + source + " into the react component: '" + name
                        + "' is a reserved name at this data level. Give it an explicit EXTID, or rename it");
        if (!names.computeIfAbsent(owner, k -> new HashSet<>()).add(name))
            throw new IllegalStateException(formErrorPrefix() + "cannot project " + source + " into the react component: '" + name
                    + "' is already projected at this data level. Give one of them an explicit EXTID, or rename it");
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

        // a GRID property is the one lsf component whose place is not its parent: its placement is DERIVED from its
        // group's (GPropertyDraw.isLsfView), so it is not asked to be a child of a react container
        if (component instanceof PropertyDrawView && ((PropertyDrawView) component).entity.isList(entity)) {
            checkLsfListView((PropertyDrawView) component);
            return;
        }

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

        // a PANEL property needs no permission from its GROUP: it is a base component of its own, drawn by the form's
        // panel controller (GFormController.getPropertyController routes every non-list draw there), not by its
        // group's. Whether React draws that group's ROWS is a question about another base component, and asking it
        // here was the collapse P1 forbids - it refused the one arrangement the principle most obviously allows:
        // React draws the rows, the platform draws one panel property of the same group, and React places it.
    }

    private void checkLsfListView(PropertyDrawView view) {
        PropertyDrawEntity<?, ?> property = view.entity;
        GroupObjectEntity toDraw = property.getToDraw(entity); // the no-arg form is unset when the group comes from the property context

        if (toDraw == null || !isReactContainerGroup(toDraw))
            throw new IllegalStateException("LSF is set for property '" + view.getSID()
                    + "', whose object group is not rendered by a CUSTOM REACT container — nothing would place the per-row renderers");

        // per-row rendering rests on the renderer key being the ROW key. With column groups the full key joins
        // row and column, so a value would be written under the joined key and read under the row key, and
        // every edit would be lost without a word
        if (!property.getColumnGroupObjects().isEmpty())
            throw new IllegalStateException("LSF is set for property '" + view.getSID()
                    + "', which is grouped in columns — it draws one editor per ROW and cannot address a row-and-column cell");
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