package lsfusion.server.logics.form.interactive.design;

import org.apache.commons.lang3.ArrayUtils;
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
        return rowsScope(group) != null;
    }

    // the component that actually DRAWS a group: the tree it belongs to, or its own grid - what a list property's
    // visibility is (FormEntity.getDrawComponent). Asking the group's BOX instead does not work for a tree - one box
    // draws all of the tree's groups, so groupObjectBox names none of them - and it answers for the box rather than for
    // what draws the group, which differ once a design MOVEs the grid out of its box or marks the grid lsf (mirrors
    // GGroupObject.getDrawComponent)
    public ComponentView getDrawComponent(GroupObjectEntity group) {
        // a group in a tree has no grid of its own (GroupObjectView builds one only outside a tree)
        if (group.isInTree())
            return get(group.treeGroup);
        return get(group).grid;
    }

    // ... and WHERE ITS ROWS GO: the react container that draws them, null when the platform does. Every rule
    // about a group's node starts by asking this, so the pair is asked as one question and composed by no caller
    // (mirrors GReactFormData.rowsScope, which is where the client asks it)
    private ContainerView rowsScope(GroupObjectEntity group) {
        return contentScope(getDrawComponent(group));
    }

    // WHAT REACT KEEPS NOTHING OF as a component: the client has no controller of it, so nothing is sent for it as
    // one - neither its readers (FormEntity.getPropertyComponents) nor its visibility (FormEntity.getBaseComponents). A
    // property whose content a react container draws - its entry, or its column in the rows React draws -, all of it
    // being its property's: the platform builds no view of it as a component, so the reader of its component class -
    // besides the grid class the property carries - has nothing to be applied to; the class as the design writes it
    // still comes with the design. And a component React draws that the design does not name - a user filter's -,
    // which has no entry to go to (mirrors GReactFormData.reactKeepsNothing)
    public boolean reactKeepsNothing(ComponentView component) {
        if (component instanceof PropertyDrawView)
            return contentScope(component) != null;
        return component.getSID() == null && component.isReactDrawn();
    }

    // (mirrors GReactFormData.contentScope)
    private ContainerView contentScope(ComponentView component) {
        // a LIST property has no place of its own: it is parked in its group's generated box, which says nothing about
        // where its column is drawn, so asking THAT container answers about the wrong component - its placement is
        // DERIVED from whoever draws the rows, in the same words as the client (GReactFormData.contentScope). Asked
        // here rather than at each caller, or the rule is restated wherever a name is claimed and a caller that
        // forgets it is answered about the box. isList says there is a group to ask.
        if (component instanceof PropertyDrawView && ((PropertyDrawView) component).entity.isList(entity))
            return rowsScope(((PropertyDrawView) component).entity.getToDraw(entity));
        return component.isLsfView() ? null : component.getReactPlace(); // an lsf component's content is the platform's
    }

    // ... and WHERE ITS DESCRIPTOR ENTRY GOES, null when it has none: every component React draws or places that is not
    // a property it carries by name - a container, a grid, a tree, a toolbar, any `lsf` component, an lsf panel
    // property included - in the container that draws or places it. Not a LIST property, lsf or not: its entry is its
    // column, on the node where the rows are, claimed with the other property names. And not a react container that
    // nothing places: its caption is drawn by the platform around it, not by its own component. Nor an lsf ACTION: its
    // caption and image are its buttons' face, which the platform draws. An entry is keyed by the component's SID, so
    // one the design does not name - a user filter's - has none (mirrors GReactFormData.descriptorScope)
    private ContainerView descriptorScope(ComponentView component) {
        if (component instanceof PropertyDrawView && (((PropertyDrawView) component).entity.isList(entity) || !component.isLsfView()
                || !((PropertyDrawView) component).entity.isStaticProperty()))
            return null;
        return component.getSID() != null ? component.getReactPlace() : null;
    }
    // ... and where its NAME is claimed: where its descriptor is - and an lsf panel property's where React places it,
    // an ACTION's included, which has no descriptor: a view places it by that name (<Lsf>) and reads data[name] as the
    // descriptor of what it places (useLsf), so nothing else may be keyed by it there
    private ContainerView nameScope(ComponentView component) {
        if (component instanceof PropertyDrawView && component.isLsfView() && !((PropertyDrawView) component).entity.isList(entity))
            return component.getSID() != null ? component.getReactPlace() : null;
        return descriptorScope(component);
    }

    // a container whose SHOWIF a react view has to be told: its descriptor's `hidden`, or - for a react container - the
    // platform hiding its box, as it hides any other component's (FormEntity.getBaseComponents)
    public boolean isReactShowIfContainer(ContainerView container) {
        return container.isReact() || descriptorScope(container) != null;
    }

    // the scopes a group's node appears in - every container that draws a PART of it (mirrors the nodes
    // GReactFormData's createGroupController and createPropertyController make, and must gain a producer kind at the
    // same time they do)
    private List<ContainerView> getGroupScopes(GroupObjectEntity group) {
        List<ContainerView> scopes = new ArrayList<>();
        for (ComponentView producer : getPartProducers(group))
            addGroupScope(scopes, contentScope(producer));
        return scopes;
    }

    // THE list of components that produce a part of a group - the one place a KIND is enumerated on this side, and the
    // twin of GReactFormData's createGroupController and createPropertyController, which make the nodes: a branch that
    // gives a component a part adds it to BOTH in one commit, or the two sides answer differently. Two kinds today: the
    // component that draws the rows, and each panel property. What is deliberately absent is the chrome (toolbar,
    // filters, calculations) - it draws nothing yet.
    private List<ComponentView> getPartProducers(GroupObjectEntity group) {
        List<ComponentView> producers = new ArrayList<>();
        producers.add(getDrawComponent(group));
        for (PropertyDrawView property : getPropertiesIt())
            if (!property.entity.isList(entity) && group.equals(property.entity.getToDraw(entity)))
                producers.add(property);
        return producers;
    }

    // ... and what those producers WRITE on the node in this scope, which is what a projected name can collide with:
    // the node's own names, and the grid's where this container draws the group's rows.
    private String[] getReservedNodeNames(GroupObjectEntity group, ContainerView scope) {
        // the top level, the empty group's node: named on the controller too, so the controller's names
        if (group == null)
            return CONTROLLER_NAMES;
        String[] reserved = NODE_OWN_NAMES;
        if (rowsScope(group) == scope)
            reserved = ArrayUtils.addAll(reserved, GRID_PART_NAMES);
        return reserved;
    }
    private static void addGroupScope(List<ContainerView> scopes, ContainerView scope) {
        if (scope != null && !scopes.contains(scope))
            scopes.add(scope);
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
    // nowhere to put the recomputed template at all. Asked of what the design declares, so a container React draws,
    // where both are ignored, is refused alike
    private void checkCustomTabbed() {
        for (ComponentView component : getComponents())
            if (component instanceof ContainerView) {
                ContainerView<?> container = (ContainerView<?>) component;
                if (container.isDeclaredTabbed()
                        && (container.getCustom() != null || container.getPropertyCustom() != null))
                    throw new IllegalStateException(formErrorPrefix() + "container '" + container.getSID() + "' is both tabbed"
                            + " and drawn by custom; a container is drawn one way, so only one of the two can be given");
            }
    }

    // a literal `custom` beside a `custom` property is the pair the rest of the design uses (caption / propertyCaption):
    // the literal is what the container starts with, the property recomputes it. The one combination that is not a pair
    // is a React component, which draws the container itself and would never be handed a computed template
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

    // a react container that is itself a non-lsf child of a react container is SWALLOWED: GFormLayout.addContainers
    // skips the whole projected subtree, so no view is built for it, its `custom` is never read, and the outer
    // component is left to draw it from `data`. Its component is therefore drawn by nobody, silently - and so is any
    // lsf child it would have placed, which is why checkLsfView does not ask this again
    private void checkCustomReactSwallowed() {
        for (ComponentView component : getComponents())
            if (component instanceof ContainerView) {
                ContainerView container = (ContainerView) component;
                // climbs, so a plain box in between does not hide it
                ContainerView drawer = container.isReact() ? contentScope(container) : null;
                if (drawer != null) // drawn by another react container, not placed by it
                    throw new IllegalStateException(formErrorPrefix() + "container '" + container.getSID() + "' is the React"
                            + " component '" + container.getCustom() + "', but it is itself drawn by the React component '"
                            + drawer.getCustom() + "', which draws its children from data - so nothing would ever build it;"
                            + " set lsf = TRUE on '" + container.getSID() + "' to give it a view of its own and place it"
                            + " with <Lsf name=\"" + container.getSID() + "\"/>");
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
        // what a NODE carries, per scope and per group, because that is what a node is: the same group has a different
        // node in each container that draws a part of it, and a name collides only with what THAT node carries. The top
        // level is the node of the EMPTY group - the form level is a group with no objects - so a null group (a HashMap
        // takes one) holds the groups, the form-level properties and the component descriptors of that scope
        // data.* and data.<group>.*
        Map<ContainerView, Map<GroupObjectEntity, Set<String>>> nodeNames = new HashMap<>();
        Map<ContainerView, Map<GroupObjectEntity, Set<String>>> rowNames = new HashMap<>();  // data.<group>.list[i].*

        // a group is projected as data.<groupSID> on every scope that sees it - asked of the group, not of a box, so
        // the groups of a tree (which share one box, named by none of them) are claimed too
        for (GroupObjectView groupObject : getGroupObjectsIt()) {
            GroupObjectEntity group = groupObject.entity;
            List<ContainerView> scopes = getGroupScopes(group);
            if (!scopes.isEmpty())
                checkProjectedGroupSID(group);
            for (ContainerView scope : scopes)
                claimProjectionName(inScope(nodeNames, scope), null, group.getSID(), "object group '" + group.getSID() + "'",
                        getReservedNodeNames(null, scope));
        }

        for (ComponentView component : getComponents()) {
            ContainerView nameScope = nameScope(component);
            if (nameScope != null) // named by its kind, since that is what the author sees in the error; data-only,
                claimProjectionName(inScope(nodeNames, nameScope), null, component.getSID(), // so the shorter list
                        (component instanceof ContainerView ? "container '" : "component '") + component.getSID() + "'", TOP_NAMES);
        }
        for (PropertyDrawView property : getPropertiesIt()) {
            GroupObjectEntity group = property.entity.getToDraw(entity); // null: the form level
            // a name is claimed WHERE ITS ENTRY LANDS, which is one container: each property asks its own contentScope,
            // which for a list property answers with the grid's part. An lsf panel property has no value entry and no
            // member - its descriptor is a top-level entry, claimed with the components above - so its name lands
            // nowhere, and neither does the name of a property nothing projects
            ContainerView scope = contentScope(property);
            // ... and a property the projection carries AT ALL, by that entry or by a descriptor, is asked its shape as
            // THE PROPERTY: not because another part of its group happens to be projected, and whether it has a name or
            // not
            if (scope != null || descriptorScope(property) != null)
                checkProjectedColumns(property, group);
            String integrationSID = property.entity.getIntegrationSID();
            // an lsf ACTION has no entry at all: its caption and image are its buttons' face
            boolean lsfAction = property.isLsfView() && !property.entity.isStaticProperty();
            // ... and a property a react container carries by a name has to have one: a view reaches it by that name
            // alone. The platform's own COUNT of a list group, the pivot's, is the one it carries by none; an author's
            // NOEXTID there leaves a property nothing could reach, so it is refused. Not an lsf one: the platform draws
            // it, and an LSF column's renderers are placed by its design name
            boolean groupCount = group != null && group.count == property.entity;
            if (scope != null && !property.isLsfView() && integrationSID == null && !groupCount)
                throw new IllegalStateException(formErrorPrefix() + "cannot project property '"
                        + property.entity.getSID() + "': the react container '" + scope.getSID() + "' draws it, and a"
                        + " view reaches a property only by its name, which NOEXTID takes away. Give it an EXTID, or"
                        + " mark it lsf for the platform to draw it");
            if (scope == null || integrationSID == null || lsfAction) // nothing carries it by a name
                continue;
            String source = group == null ? "form property '" + integrationSID + "'" : "property '" + group.getSID() + "." + integrationSID + "'";
            checkProjectedName(integrationSID, source);
            // ... and it collides with the names that SCOPE carries, not with the ones another container does: a
            // container holding one panel property of the group has no `list` for a property called `list` to take,
            // while a panel property MOVEd in beside the grid does meet it
            claimProjectionName(inScope(nodeNames, scope), group, integrationSID, source, getReservedNodeNames(group, scope));
            // an LSF list property has no per-row cell, only its column
            if (property.entity.isList(entity) && !property.isLsfView())
                claimProjectionName(inScope(rowNames, scope), group, integrationSID, source, ROW_NAMES);
        }
    }

    private static Map<GroupObjectEntity, Set<String>> inScope(Map<ContainerView, Map<GroupObjectEntity, Set<String>>> names, ContainerView scope) {
        return names.computeIfAbsent(scope, k -> new HashMap<>());
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
    // what a controller carries beside its members: the four verbs every controller has (GController.extendController),
    // the editing a view declares (GFormController.extendController) and the batch that is the members' shortcut. A
    // projected name equal to one of them would have to be dropped, and the verbs are how everything outside this
    // surface is reached
    private static final String[] CONTROLLER_NAMES = {"exec", "eval", "evalAction", "change", "startEditing", "stopEditing",
            "isEditing", "properties", PROTO};
    // what a group's node carries besides the properties, ONE LINE PER PRODUCER: a branch that gives a component a
    // part of its own adds its line here rather than editing another's, so two of them merge instead of colliding.
    // A name in this array is claimed for the node's DATA and for the group's controller MEMBERS alike - `change` is
    // the group's own verb - so a feature that writes on the node has one array to update, not two.
    private static final String[] GRID_PART_NAMES = {
            "list", "byKey", "keys", "options",                 // the grid's part
    };
    // Every group's node reserves them, whatever it carries in this scope: `change` is the verb of a group whose rows
    // are drawn here, and a name valid with the rows in one container stays valid when they move to another; `__member`
    // is the stamp GReactFormData.makePropertyMember gives a property member - non-enumerable and non-writable -, kept
    // the members' own on the group's member too
    private static final String[] NODE_OWN_NAMES = {
            "properties", "change", PROTO, "__member", // the node's own: its index, its verb, the stamp
    };
    private static final String[] ROW_NAMES = {
            "key", "isCurrent", "objects", "background", "foreground", "selected", PROTO, // the grid's part: a ROW
    };

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

    // a projected name is a path a view WRITES - data.<group>.<name>, controller.<group>.<name> - and the one surface
    // that still reads a qualified name, the classic changeProperty, reads `<groupSID>.<integrationSID>` split at the
    // LAST dot (a state verb's `property` is a name the view's own group carries). A group SID never carries one
    // (checkProjectedGroupSID refuses an unnamed group of several objects), so the split is unambiguous only while the
    // PROPERTY half carries none either - and an EXTID is an arbitrary string, so a dot in one would be read as a group
    // prefix and the name would ask for a group that does not exist. Asked where the name lands, and only there
    private void checkProjectedName(String integrationSID, String source) {
        if (integrationSID.indexOf('.') >= 0)
            throw new IllegalStateException(formErrorPrefix() + "cannot project " + source
                    + ": its integration name carries a dot, and a name is read as '<object group>.<property>' -"
                    + " everything before the LAST dot is the group. Give it an EXTID without a dot");
    }

    // a property GROUPED IN COLUMNS is one cell per row AND column, and everything a react view has of a property means
    // one ROW: its name, in `data` and on the controller, and its descriptor, read at one key while the platform's own
    // renderer gives its caption up to it. Where the projection CARRIES the property, that is not a runtime surprise to
    // report but a form that cannot mean what it says, so it is refused here. Only there: a columns property nothing
    // projects is nobody's business, and the classic surface that still names it (its own stringly-typed
    // changeProperty, #1655) refuses it when the call is made. What this DOES settle is what the projection itself
    // hands out: inside it a name means one ordinary property, and a react view's own state reads those names without
    // asking again.
    private void checkProjectedColumns(PropertyDrawView property, GroupObjectEntity group) {
        if (!property.entity.getColumnGroupObjects().isEmpty())
            throw new IllegalStateException(formErrorPrefix() + "cannot project " + (group == null ? "form property"
                    : "property of object group '" + group.getSID() + "'") + " '" + property.getSID()
                    + "': it is grouped in COLUMNS - one cell per row AND column - and a react view names one ROW."
                    + " Draw it as an ordinary property, or keep it out of what a react container projects");
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

    // the design is complete here, so `lsf` can finally be checked against the container tree
    private void checkLsfViews() {
        for (ComponentView component : getComponents()) // the property draws are components too, so this covers them
            checkLsfView(component);
    }

    private void checkLsfView(ComponentView component) {
        if (!component.isLsfView())
            return;

        // a GRID property is the one lsf component whose place is not where it stands: its placement is DERIVED from
        // its group's - the view that draws the rows places its renderers - so it is asked that, not to have a react
        // container above it, wherever its own component is: a list property is parked in its group's box, and a
        // REMOVEd box says nothing about where the rows are drawn. The client takes the flag as checked here
        // (GComponent.isLsfView)
        if (component instanceof PropertyDrawView && ((PropertyDrawView) component).entity.isList(entity)) {
            checkLsfListView((PropertyDrawView) component);
            return;
        }

        if (!component.isInForm()) // REMOVEd, or inside what was: nothing draws or places it
            return;

        // placed by the react container above it, however deep under the containers that one draws itself
        // (getReactPlace); under an lsf container there is nothing to place - the platform draws that container whole
        if (component.getReactPlace() == null)
            throw new IllegalStateException("lsf is set for '" + component.getSID() + "', which no CUSTOM REACT container places:"
                    + " there is none above it, or an lsf container on the way, which the platform draws whole");

        // a PANEL property needs no permission from its GROUP: it is a base component of its own, drawn by the form's
        // panel controller (GFormController.getLsfPropertyController routes every non-list property there), not by its
        // group's. Whether React draws that group's ROWS is a question about another base component, and asking it
        // here decided one base component by another's answer - it refused the arrangement most plainly right:
        // React draws the rows, the platform draws one panel property of the same group, and React places it.
    }

    private void checkLsfListView(PropertyDrawView view) {
        PropertyDrawEntity<?, ?> property = view.entity;
        // the no-arg form is unset when the group comes from the property context
        GroupObjectEntity toDraw = property.getToDraw(entity);

        if (!isReactContainerGroup(toDraw)) // a LIST property has a group to draw it (PropertyDrawEntity.isList)
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