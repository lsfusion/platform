package lsfusion.server.logics.form.interactive.design;

import org.apache.commons.lang3.ArrayUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.design.ReactProjectionNames;
import lsfusion.interop.form.event.*;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.server.base.Custom;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;
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
        if (propertyDrawView.filter == null)
            getDrawComponent(filterProperty.getNFToDraw(entity, version)).addFilter(genID(), propertyDrawView, version);
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

    // a group whose rows a React view draws - a container's, or its grid's own - is drawn by React, not the standard
    // grid, so the server must not apply grid-only behavior to its properties (e.g. autoselect, which would turn a
    // foreign-key column's value into a JSON candidate list). A box with `custom = NULL` keeps its standard grid, so
    // its group is not React-owned
    public boolean isReactDrawnGroup(GroupObjectEntity group) {
        return rowsScope(group) != null;
    }

    // the component that actually DRAWS a group: the tree it belongs to, or its own grid - what a list property's
    // visibility is (FormEntity.getDrawComponent). Asking the group's BOX instead does not work for a tree - one box
    // draws all of the tree's groups, so groupObjectBox names none of them - and it answers for the box rather than for
    // what draws the group, which differ once a design MOVEs the grid out of its box or gives the grid `custom = NULL` (mirrors
    // GGroupObject.getDrawComponent)
    public GridPropertyView<?, ?> getDrawComponent(GroupObjectEntity group) {
        // a group in a tree has no grid of its own (GroupObjectView builds one only outside a tree)
        if (group.isInTree())
            return get(group.treeGroup);
        return get(group).grid;
    }

    // ... and WHERE ITS ROWS GO: the React view that draws them, null when the platform does - none for a group not in
    // LIST view (PANEL, TOOLBAR, POPUP), which is sent no rows. Every rule about a group's node starts by asking this,
    // so the pair is asked as one question and composed by no caller (mirrors GReactFormData.rowsScope, which is where
    // the client asks it)
    private ComponentView rowsScope(GroupObjectEntity group) {
        return !group.getViewType().isList() ? null : componentScope(getDrawComponent(group));
    }

    // WHAT REACT KEEPS NOTHING OF as a component: the client has no controller of it, so nothing is sent for it as
    // one - neither its readers (FormEntity.getPropertyComponents) nor its visibility (FormEntity.getBaseComponents): a
    // component React draws that the design does not name - a user filter's -, which has no entry to go to. Not a
    // property, whatever draws it: as a component it is the platform's, and the one reader it has as one is its class
    // (a design showIf on a property is the property's SHOWIF), which the platform applies to the view it draws the
    // property in and skips where React draws it (mirrors GReactFormData.reactKeepsNothing)
    public boolean reactKeepsNothing(ComponentView component) {
        return component.getSID() == null && component.isReactDrawn();
    }

    // where what a COMPONENT draws is drawn - its content: the React view that draws it, its own where its custom is a
    // React component, null when the platform does - a platform-drawn one too, whose content is the platform's wherever
    // React places it (mirrors GReactFormData.componentScope)
    private ComponentView componentScope(ComponentView component) {
        // a LIST property has no place of its own: it is parked in its group's generated box, which says nothing about
        // where its column is drawn, so asking THAT container answers about the wrong component - its placement is
        // DERIVED from whoever draws the rows, in the same words as the client (GReactFormData.componentScope). Asked
        // here rather than at each caller, or the rule is restated wherever a name is claimed and a caller that
        // forgets it is answered about the box. isList says there is a group to ask.
        if (component instanceof PropertyDrawView && ((PropertyDrawView) component).entity.isList(entity))
            return rowsScope(((PropertyDrawView) component).entity.getToDraw(entity));
        return component.getContentReactPlace();
    }

    // PLATFORM-DRAWN: a component the platform draws and a React view places - in a <Platform> - instead of drawing it:
    // one with a view of its own (ComponentView.hasExplicitCustom) where a React view draws what it stands in - a list
    // property where React draws its group's rows, any other component under a React view. What the client is told of
    // a component (GComponent.isPlatformDrawn): anywhere else such a component is drawn where it stands, as usual. A
    // view of its own that is a React component is drawn by React, and still placed by the one above it
    public boolean isPlatformDrawn(ComponentView component) {
        if (!component.hasExplicitCustom())
            return false;
        if (component instanceof PropertyDrawView && ((PropertyDrawView) component).entity.isList(entity))
            return rowsScope(((PropertyDrawView) component).entity.getToDraw(entity)) != null;
        return component.getReactPlace() != null;
    }

    // ... and WHERE THE VIEW NAMES IT, null where none does: the one container a component's name is claimed in, and
    // where its entry is. A property is named by its integration name on its group's node, where React draws its
    // content - a list property's column, platform-drawn or not, where the rows are - or, for a platform-drawn panel
    // one, where React places it, its entry labelling what the platform draws (an ACTION's saying only whether the form
    // shows it: its caption and image are its button's face). Any other component is named by its design identifier at
    // the top level, where React draws or places it - one the design does not name, a user filter's, nowhere; nor a
    // React view nothing places, whose caption the platform draws around it (mirrors GReactFormData.nameScope)
    private ComponentView nameScope(ComponentView component) {
        if (component instanceof PropertyDrawView && ((PropertyDrawView) component).entity.isList(entity)) // its column
            return rowsScope(((PropertyDrawView) component).entity.getToDraw(entity));
        return component.getSID() != null ? component.getReactPlace() : null;
    }

    // the name a view has for a property: its integration name, after its group's - o.note -, alone for the empty
    // group's (mirrors GReactFormData.getName)
    private String getName(PropertyDrawView property) {
        GroupObjectEntity group = property.entity.getToDraw(entity);
        String integrationSID = property.entity.getIntegrationSID();
        return group == null ? integrationSID : group.getSID() + "." + integrationSID;
    }

    // a container whose SHOWIF a React view has to be told: its descriptor's `hidden`, or - for a react container - the
    // platform hiding its box, as it hides any other component's (FormEntity.getBaseComponents)
    public boolean isReactShowIfContainer(ContainerView container) {
        return container.isReact() || nameScope(container) != null;
    }

    // the scopes a group's node appears in - every React view that draws a part of it: its rows; each panel property
    // with an entry, one React draws or one it places (platform-drawn); each filter group of the group,
    // FILTERGROUP(fg), whose part is the filter selected in it; and, of a list's group, the parts of its TABLE, its
    // grid (addTableScopes). The twin of GReactFormData's createGridController and createTreeController,
    // createPropertyController, createFilterGroupController and the factories of a table's parts, which make the nodes,
    // each kind asked here by the scope its factory asks - rowsScope, nameScope, filterGroupScope and a table's, the
    // twins of the client's -: a branch that gives a component a part adds it to BOTH in one commit, or the two sides
    // answer differently. What is deliberately absent is the group's calculations (CalculationsView), which React draws
    // nothing of yet, and a tree's table, one for all its groups, whose parts are on the tree's node (getTreeScopes)
    private List<ComponentView> getGroupScopes(GroupObjectEntity group) {
        List<ComponentView> scopes = new ArrayList<>();
        addScope(scopes, rowsScope(group));
        for (PropertyDrawView property : getPropertiesIt())
            if (!property.entity.isList(entity) && group.equals(property.entity.getToDraw(entity))
                    && hasEntry(property))
                addScope(scopes, nameScope(property));
        for (RegularFilterGroupView filterGroup : getRegularFiltersIt())
            if (group.equals(filterGroup.entity.getToDraw(entity)))
                addScope(scopes, filterGroupScope(filterGroup));
        GridView grid = getListGrid(group);
        if (grid != null)
            addTableScopes(scopes, grid);
        return scopes;
    }
    // ... and the scopes a TREE's node appears in: those of the parts of its table, one for all its groups
    // (addTableScopes)
    private List<ComponentView> getTreeScopes(TreeGroupView tree) {
        List<ComponentView> scopes = new ArrayList<>();
        addTableScopes(scopes, tree);
        return scopes;
    }
    // ... the scopes of the parts of a TABLE - a list's grid, or a tree -, on its node: its GRID, where its rows are
    // drawn, with its sortings - a tree's part being the tree as a whole, as a group's node is where its rows are -;
    // what its FILTERS box holds, the user filters of its groups; and its FILTERCONTROLS and its TOOLBARSYSTEM, shown,
    // whose parts are their buttons (the twin of the factories of a table's parts: GReactFormData.createGridController,
    // createTreeController (with the table's sortings), createFiltersControllers, createFilterControlsController,
    // createGridToolbarController, createTreeToolbarController)
    private void addTableScopes(List<ComponentView> scopes, GridPropertyView<?, ?> table) {
        addScope(scopes, componentScope(table));
        addScope(scopes, filtersScope(table.filtersContainer));
        addScope(scopes, filterControlsScope(table));
        addScope(scopes, toolbarScope(table));
    }
    private static void addScope(List<ComponentView> scopes, ComponentView scope) {
        if (scope != null && !scopes.contains(scope))
            scopes.add(scope);
    }
    // whether a property the view names has an entry there: one it carries by a name - a platform-drawn ACTION too,
    // whose entry says whether the form shows it -, not the one React draws by none, the group's own COUNT. A part of a
    // group is a property with an entry (mirrors GReactFormData.hasEntry)
    private boolean hasEntry(PropertyDrawView property) {
        return property.entity.getIntegrationSID() != null;
    }
    // the group's FILTERS box: its grid's, or its tree's - one box for all the tree's groups
    private ContainerView getFiltersContainer(GroupObjectEntity group) {
        return getDrawComponent(group).filtersContainer;
    }
    // where what a FILTERS box holds is drawn - the box's part being its content -: the React view that draws it, null
    // elsewhere (mirrors GReactFormData.filtersScope)
    private ComponentView filtersScope(ContainerView box) {
        return box.getContentReactPlace();
    }
    // ... and where a GROUP's user filters are drawn as a part: what its FILTERS box holds - its grid's, or its tree's,
    // one box for all the tree's groups -; none of a group in PANEL view, whose user filters nothing shows, as the
    // platform shows none for it (mirrors where GFormController.initializeTable asks GReactFormData for that part, and
    // initializeControllers gives a group in PANEL view none)
    private ComponentView userFiltersScope(GroupObjectEntity group) {
        if (!group.isInTree() && !group.getViewType().isList())
            return null;
        return filtersScope(getFiltersContainer(group));
    }
    // ... where a FILTER GROUP is drawn as a part of its group: one with filters - one with none has no view -, of a
    // group - none has no node of its own to go on -, named on its group's node there by its own name (mirrors
    // GReactFormData.filterGroupScope)
    private ComponentView filterGroupScope(RegularFilterGroupView filterGroup) {
        if (filterGroup.filters.getList().isEmpty())
            return null;
        return filterGroup.entity.getToDraw(entity) != null ? componentScope(filterGroup) : null;
    }
    // ... where the FILTERCONTROLS of a table or a tree is drawn as a part of its table - a list's grid
    // (getListGrid), or a tree, one for all its groups (mirrors GReactFormData.filterControlsScope)
    private ComponentView filterControlsScope(GridPropertyView<?, ?> table) {
        return componentScope(table.filterControls);
    }
    // ... and where its TOOLBARSYSTEM is, shown (mirrors GReactFormData.toolbarScope)
    private ComponentView toolbarScope(GridPropertyView<?, ?> table) {
        return table.toolbarSystem.isVisible() ? componentScope(table.toolbarSystem) : null;
    }
    // ... the grid of a group whose FILTERCONTROLS and TOOLBARSYSTEM go over its rows: a list's not in a tree, none of
    // a panel's, which shows no rows, nor of a tree's group, whose tree has them (getTreeScopes) - the grids
    // GFormController.initializeGrid sets them up for
    private GridView getListGrid(GroupObjectEntity group) {
        return group.getViewType().isList() && !group.isInTree() ? get(group).grid : null;
    }

    // ... and what those parts WRITE on the node in this scope, which is what a projected name can collide with: the
    // node's own names, and the names of the parts of the group placed in this container - asked once per place a part
    // goes to (mirrors where GReactFormData's createGridController, createTreeController - with the table's sortings -,
    // createFiltersControllers, createFilterControlsController and createGridToolbarController place the parts; a
    // filter group's part is named by its own name, claimed as a property's is)
    private String[] getReservedNodeNames(GroupObjectEntity group, ComponentView scope) {
        // the top level, the empty group's node: named on the controller too, so the controller's names
        if (group == null)
            return CONTROLLER_NAMES;
        String[] reserved = NODE_OWN_NAMES;
        if (rowsScope(group) == scope) { // where the rows are drawn: the rows' part, a tree group's too
            reserved = ArrayUtils.addAll(reserved, GRID_PART_NAMES);
            if (group.isInTree())
                reserved = ArrayUtils.addAll(reserved, TREE_PART_NAMES);
        }
        // ... and the parts of a list's table, on its group's node - a tree's are on the tree's node, which carries no
        // property: the sortings, where its rows are drawn; what FILTERS(g) holds; FILTERCONTROLS(g); TOOLBARSYSTEM(g),
        // its buttons
        GridView grid = getListGrid(group);
        if (grid != null) {
            if (rowsScope(group) == scope)
                reserved = ArrayUtils.addAll(reserved, ORDERS_PART_NAMES);
            if (filtersScope(grid.filtersContainer) == scope)
                reserved = ArrayUtils.addAll(reserved, FILTERS_PART_NAMES);
            if (filterControlsScope(grid) == scope)
                reserved = ArrayUtils.addAll(reserved, FILTER_CONTROLS_PART_NAMES);
            if (toolbarScope(grid) == scope)
                reserved = ArrayUtils.addAll(reserved, TOOLBAR_PART_NAMES);
        }
        return reserved;
    }
    // ... and what a ROW of it carries - asked only where the rows are drawn, the one scope a column is claimed in:
    // the grid's names, and a tree's beside them
    private String[] getReservedRowNames(GroupObjectEntity group) {
        String[] reserved = GRID_ROW_NAMES;
        if (group.isInTree())
            reserved = ArrayUtils.addAll(reserved, TREE_ROW_NAMES);
        return reserved;
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

        mainContainer.finalizeAroundInit();

        for(RegularFilterGroupView regularFilter : getRegularFiltersIt())
            regularFilter.finalizeAroundInit();

        for(ComponentView component : getComponents())
            component.finalizeAroundInit();

        pivotColumns.finalizeChanges();
        pivotRows.finalizeChanges();
        pivotMeasures.finalizeChanges();

        components.finalizeChanges();

        // what draws a component comes first: every check below reads it - whether a name is projected, what a place
        // may name - so a container whose custom is not a renderer at all, or a React view given nothing to draw, would
        // fail one of them instead, naming a consequence rather than the mistake
        checkCustomValue();
        checkCustomTabbed();
        checkCustomReactProperty();
        checkCustomTemplatePlaces();
        checkReactRowsListView();
        checkCustomReactComponents();

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
    // nowhere to put the recomputed template at all. Asked wherever the container is: inside what a React component
    // draws the container's custom is a view of its own, which that component places
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

    // a group whose rows a React view draws is a list of rows: the server starts a group in another list view - PIVOT,
    // MAP, CALENDAR, CUSTOM - as the platform's view of that kind needs it (a pivot's group mode, or under asyncInit
    // waiting for the pivot to ask for it; a calendar's or a map's page size), which a view drawing the rows never asks
    // for nor undoes. Refused when the form is built, rather than the view getting rows it did not ask for - or none
    private void checkReactRowsListView() {
        for (GroupObjectView groupObject : getGroupObjectsIt()) {
            GroupObjectEntity group = groupObject.entity;
            ListViewType listViewType = group.getListViewTypeValue();
            ComponentView rows = rowsScope(group);
            if (listViewType != ListViewType.GRID && rows != null)
                throw new IllegalStateException(formErrorPrefix() + "the rows of group '" + group.getSID() + "' are drawn by"
                        + " the React view of '" + rows.getSID() + "', but the group is in "
                        + listViewType.name() + " view, which the platform draws; a React view draws the rows in the"
                        + " default GRID view - drop the view type, or give what draws the rows, the group's grid or its tree,"
                        + " `custom = NULL` for the platform to draw them");
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

    // a component that is not a container is drawn by a React component of its own as what such a view draws of it:
    // the part of a group or of a tree it shows - the rows of a grid or of a tree, the toolbar, the filter controls, a
    // filter group -, on the node of its owner (getGroupScopes, getTreeScopes). One that shows none here - of a group
    // in PANEL view, which has no rows, toolbar or filter controls; a toolbar hidden with visible = FALSE; a filter
    // group with no filters or of no object; a group's calculations, which React draws nothing of - would be drawn by a
    // view given nothing, so it is refused. And so is a grid whose record holds anything - what the design moves into
    // the grid, GRID(o) { MOVE ...; }, which a CUSTOM, MAP or CALENDAR view pops up for a row and rows React draws never
    // show (checkReactRowsListView) -: such a view places no view of the platform's, as a container's places those it
    // holds, so what the record holds is a React container's around the grid, to draw or to place
    private void checkCustomReactComponents() {
        Set<ComponentView> scopes = new HashSet<>();
        for (GroupObjectView groupObject : getGroupObjectsIt())
            scopes.addAll(getGroupScopes(groupObject.entity));
        for (TreeGroupView tree : getTreeGroupsIt())
            scopes.addAll(getTreeScopes(tree));
        for (ComponentView component : getComponents())
            if (!(component instanceof ContainerView) && component.isReact() && component.isInForm()) {
                if (!scopes.contains(component))
                    throw new IllegalStateException(formErrorPrefix() + "'" + component.getSID() + "' is drawn by the React"
                            + " component '" + component.getCustom() + "', which would be given nothing to draw: the React"
                            + " view of a grid, a tree, a toolbar, filter controls or a filter group draws what that component"
                            + " shows of its group or its tree, and this one shows nothing (a group in PANEL view has no rows,"
                            + " toolbar or filter controls; a toolbar hidden with visible = FALSE and a filter group with no"
                            + " filters or of no object have nothing to show)");
                ContainerView record = component instanceof GridView ? ((GridView) component).getRecord() : null;
                if (record != null && !record.getChildrenList().isEmpty())
                    throw new IllegalStateException(formErrorPrefix() + "'" + component.getSID() + "' is drawn by the React"
                            + " component '" + component.getCustom() + "', and the design moves components into it - into"
                            + " the record a CUSTOM, MAP or CALENDAR view pops up for a row -, which the React view of the"
                            + " grid itself does not take; move them out, or draw the grid with a container: NEW board { custom = '"
                            + component.getCustom() + "'; MOVE " + component.getSID() + "; }");
            }
    }

    // a child drawn inline gives a place to each of its parts - sID.caption, sID.comment - so a name belongs to a
    // component when it is its sID or names a part below it. Only the sID: a property is named as the design names it,
    // PROPERTY(qty), and not by the property alone - one name, one spelling, and no question of which child answers
    // when a container and a property could both be read as one
    private static boolean names(String name, String sid) {
        return name.equals(sid) || name.startsWith(sid + ".");
    }

    // A container drawn by an HTML template gives each child it shows a <Platform:sID> place. A place naming nothing the form
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

    // A React view projects its groups and properties into a JS `data` object (see the web client's GReactFormData),
    // which owns some names at every level. A projected integration SID that takes one of them, or that two projected
    // items share, would silently overwrite the other — so reject it here, with the rest of the design checks: the
    // whole form is then rejected once, when it is built, instead of failing in every browser that opens it. The names
    // below mirror what GReactFormData writes; keep them in sync with it.
    private void checkReactProjectionNames() {
        // what a NODE carries, per scope and per group, because that is what a node is: the same group has a different
        // node in each container that draws a part of it, and a name collides only with what THAT node carries. The top
        // level is the node of the EMPTY group - the form level is a group with no objects - so a null group (a HashMap
        // takes one) holds the groups, the form-level properties and the component descriptors of that scope
        // data.* and data.<group>.*
        Map<ComponentView, Map<GroupObjectEntity, Set<String>>> nodeNames = new HashMap<>();
        Map<ComponentView, Map<GroupObjectEntity, Set<String>>> rowNames = new HashMap<>();  // data.<group>.list[i].*

        // a group is projected as data.<groupSID> on every scope that sees it - asked of the group, not of a box, so
        // the groups of a tree (which share one box, named by none of them) are claimed too
        for (GroupObjectView groupObject : getGroupObjectsIt()) {
            GroupObjectEntity group = groupObject.entity;
            List<ComponentView> scopes = getGroupScopes(group);
            if (!scopes.isEmpty())
                checkProjectedGroupSID(group);
            for (ComponentView scope : scopes)
                claimProjectionName(inScope(nodeNames, scope), null, group.getSID(), "object group '" + group.getSID() + "'",
                        getReservedNodeNames(null, scope));
        }

        // ... and a tree as data.<tree>, by its name in the form, on every scope that draws a part of it - a node
        // beside the groups', so its name may take neither a group's nor a property's of the empty group. Nothing else
        // is named on it: a property is of a group, and the parts it carries write names of their own
        for (TreeGroupView tree : getTreeGroupsIt()) {
            String name = tree.entity.getSID();
            String source = "tree '" + name + "'";
            for (ComponentView scope : getTreeScopes(tree))
                claimProjectionName(inScope(nodeNames, scope), null, name, source, getReservedNodeNames(null, scope));
            // ... but a condition of its user filters names a property by its group's name, one word - read up to its
            // first dot (GReactFormData.TreeNode.readPropertyName) -, whether or not a view draws a part of the
            // group itself
            if (filtersScope(tree.filtersContainer) != null)
                for (GroupObjectEntity group : tree.entity.getGroups())
                    checkProjectedGroupSID(group);
        }

        for (ComponentView component : getComponents()) {
            if (component instanceof PropertyDrawView) // named by its integration name on its group's node: below
                continue;
            ComponentView nameScope = nameScope(component);
            if (nameScope != null) // named by its kind, since that is what the author sees in the error; data-only,
                claimProjectionName(inScope(nodeNames, nameScope), null, component.getSID(), // so the shorter list
                        (component instanceof ContainerView ? "container '" : "component '") + component.getSID() + "'", TOP_NAMES);
        }
        for (PropertyDrawView property : getPropertiesIt()) {
            GroupObjectEntity group = property.entity.getToDraw(entity); // null: the form level
            // a name is claimed WHERE THE VIEW NAMES THE PROPERTY, which is one container: each property asks its own
            // nameScope, which for a list property answers with the grid's part, and for a platform-drawn panel
            // property with where React places it. The name of a property nothing projects lands nowhere
            ComponentView scope = nameScope(property);
            // ... and a property the projection carries AT ALL is asked its shape as THE PROPERTY: not because another
            // part of its group happens to be projected, and whether it has a name or not
            if (scope != null)
                checkProjectedColumns(property, group);
            String integrationSID = property.entity.getIntegrationSID();
            // ... and a property a React view draws or places has to have a name: a view reaches it by that name
            // alone - its value, its entry, its member, the <Platform> that places it. The platform's own COUNT of a
            // list group, the pivot's, is the one it carries by none; an author's NOEXTID there leaves a property
            // nothing could reach, so it is refused
            boolean groupCount = group != null && group.count == property.entity;
            if (scope != null && integrationSID == null && !groupCount)
                throw new IllegalStateException(formErrorPrefix() + "cannot project property '"
                        + property.entity.getSID() + "': the React view of '" + scope.getSID() + "' "
                        + (isPlatformDrawn(property) ? "places" : "draws") + " it, and a view reaches a property only"
                        + " by its name, which NOEXTID takes away. Give it an EXTID");
            if (scope == null || integrationSID == null) // nothing names it
                continue;
            String source = (group == null ? "form property '" : "property '") + getName(property) + "'";
            checkProjectedName(integrationSID, source);
            // ... and it collides with the names that SCOPE carries, not with the ones another container does: a
            // container holding one panel property of the group has no `list` for a property called `list` to take,
            // while a panel property MOVEd in beside the grid does meet it
            claimProjectionName(inScope(nodeNames, scope), group, integrationSID, source, getReservedNodeNames(group, scope));
            // a platform-drawn list property has no per-row cell, only its column
            if (property.entity.isList(entity) && !isPlatformDrawn(property))
                claimProjectionName(inScope(rowNames, scope), group, integrationSID, source,
                        getReservedRowNames(group));
        }

        // a filter group a React view draws: named on its group's node there by its own name, as a property is by
        // its integration name (GReactFormData.FilterGroupPart)
        for (RegularFilterGroupView filterGroup : getRegularFiltersIt()) {
            ComponentView scope = filterGroupScope(filterGroup);
            if (scope == null)
                continue;
            GroupObjectEntity group = filterGroup.entity.getToDraw(entity);
            String name = filterGroup.entity.getSID();
            String source = "filter group '" + name + "'";
            claimProjectionName(inScope(nodeNames, scope), group, name, source, getReservedNodeNames(group, scope));
        }

        // ... and where a group's state is projected - its sortings, with its rows, its user filters, or its toolbar's
        // sum, which names a column -, a name the group carries on two properties anywhere - asked once the names are
        // claimed, so a name two projected properties share on one node is told as that
        for (GroupObjectView groupObject : getGroupObjectsIt()) {
            GroupObjectEntity group = groupObject.entity;
            GridView grid = getListGrid(group);
            if (rowsScope(group) != null || userFiltersScope(group) != null
                    || grid != null && toolbarScope(grid) != null)
                checkStateNames(group);
        }
    }

    private static Map<GroupObjectEntity, Set<String>> inScope(Map<ComponentView, Map<GroupObjectEntity, Set<String>>> names, ComponentView scope) {
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
    // what a component's descriptor is claimed against at the top level: data alone - a component has no member
    private static final String[] TOP_NAMES = {
            PROTO,
    };
    // what a controller carries beside its members: the four verbs every controller has (GController.extendController),
    // the editing a view declares (GFormController.extendController) and the batch the members' changes are sent
    // together in. A projected name equal to one of them would have to be dropped, and the verbs are how everything
    // outside this surface is reached
    private static final String[] CONTROLLER_NAMES = reserved(ReactProjectionNames.CONTROLLER, PROTO);
    // what every group's node reserves, whatever it carries in this scope - its index, its verb: `change` is the verb
    // of a group whose rows are drawn here, and a name valid with the rows in one container stays valid when they move
    // to another
    private static final String[] NODE_OWN_NAMES = reserved(ReactProjectionNames.NODE_OWN, PROTO);
    // ... and what each PART of a group writes beside the properties, in the container the part is in - on the group's
    // node (_PART_NAMES), and on a row of it where it has rows (_ROW_NAMES) -, each part with arrays of its own: a
    // branch that gives a component a part adds its arrays rather than editing another's, so two of them merge instead
    // of colliding. A name on the node is claimed for its DATA and for the group's controller MEMBERS alike - a part's
    // member hangs on the group's -, so a part that writes on the node has one array to update, not two. First the
    // rows' part of a group (GReactFormData.RowsPart), a tree's group's too, where the rows are drawn
    private static final String[] GRID_PART_NAMES = reserved(ReactProjectionNames.GRID_PART);
    private static final String[] GRID_ROW_NAMES = reserved(ReactProjectionNames.GRID_ROW, PROTO);
    // ... and of a group of a tree (GReactFormData.TreeRowsPart): its node verbs, and where each row hangs - not the
    // tree's own part (TreeTablePart), which carries no property. A group outside a tree states none of it, and a
    // property of one may take these names
    private static final String[] TREE_PART_NAMES = reserved(ReactProjectionNames.TREE_PART);
    private static final String[] TREE_ROW_NAMES = reserved(ReactProjectionNames.TREE_ROW);
    // the sortings' part, placed with the rows - the order they are drawn in -, as a group's design has no box of its
    // sortings to place them by
    private static final String[] ORDERS_PART_NAMES = reserved(ReactProjectionNames.ORDERS_PART);
    // the user filters' part, where what FILTERS(g) holds is drawn: the conditions, and what a condition can be on
    private static final String[] FILTERS_PART_NAMES = reserved(ReactProjectionNames.FILTERS_PART);
    // ... their controls' part, where FILTERCONTROLS(g) is drawn: the same whoever draws the conditions - the add, the
    // apply and the reset, and what the apply button shows (mirrors GReactFormData.FilterControlsPart)
    private static final String[] FILTER_CONTROLS_PART_NAMES = reserved(ReactProjectionNames.FILTER_CONTROLS_PART);
    // the toolbar's part, where TOOLBARSYSTEM(g) is drawn: what its buttons put on the group's node - each its state
    // and its member under one name, or its member alone -, all of them, whatever the design's flags say, which are the
    // platform's alone (mirrors GReactFormData.GridToolbarPart): the count and the sum, the export, the manual update,
    // the filter button, the view switch
    private static final String[] TOOLBAR_PART_NAMES = reserved(ReactProjectionNames.TOOLBAR_PART);
    // a list of the names the projection writes (ReactProjectionNames), and the ones given beside it
    private static String[] reserved(String names, String... more) {
        return ArrayUtils.addAll(names.split(","), more);
    }

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

    // a STATE of a group - a sorting, a user-filter condition, a column its toolbar sums - names a property of the
    // GROUP by its integration SID, wherever the property is drawn: a state's part need not be where an entry of it is
    // - FILTERS(g) with a view of its own over the platform's grid is where no node names a column, and the node of the
    // rows carries no panel property drawn elsewhere - so no node's names settle what a state means
    // (GReactFormData.readStateProperty). Where a part of the group's state is projected, a name it carries on two
    // properties would name nothing: refused here, like any other name the projection could not mean, rather than left
    // to a view that finds its sorting or its condition unlisted
    private void checkStateNames(GroupObjectEntity group) {
        Set<String> names = new HashSet<>();
        for (PropertyDrawView property : getPropertiesIt()) {
            String integrationSID = property.entity.getIntegrationSID();
            if (integrationSID != null && group.equals(property.entity.getToDraw(entity)) && !names.add(integrationSID))
                throw new IllegalStateException(formErrorPrefix() + "cannot project the sortings, the user filters or"
                        + " the toolbar of object group '" + group.getSID() + "': '" + integrationSID + "' is drawn more"
                        + " than once on it, and a sorting, a condition or the toolbar's sum names a property of the group"
                        + " by that name. Give them explicit EXTIDs");
        }
    }

    // a projected name is a path a view WRITES - data.<group>.<name>, controller.<group>.<name>,
    // <Platform name="o.note"/> - and what reads one qualified - a <Platform> given a row, the classic changeProperty -
    // splits `<groupSID>.<integrationSID>` at the LAST dot (a state verb's `property` is a name the view's own group
    // carries). A group SID never carries one (checkProjectedGroupSID refuses an unnamed group of several objects), so
    // the split is unambiguous only while the PROPERTY half carries none either - and an EXTID is an arbitrary string,
    // so a dot in one would be read as a group prefix and the name would ask for a group that does not exist. Nor may
    // an EXTID be empty: a <Platform> with no name places nothing, so a property of the empty group would be one no
    // view can place. Asked where the name lands, and only there
    private void checkProjectedName(String integrationSID, String source) {
        if (integrationSID.isEmpty())
            throw new IllegalStateException(formErrorPrefix() + "cannot project " + source + ": its integration name is"
                    + " empty, and a view reaches a property only by its name. Give it a non-empty EXTID");
        if (integrationSID.indexOf('.') >= 0)
            throw new IllegalStateException(formErrorPrefix() + "cannot project " + source
                    + ": its integration name carries a dot, and a name is read as '<object group>.<property>' -"
                    + " everything before the LAST dot is the group. Give it an EXTID without a dot");
    }

    // a property GROUPED IN COLUMNS is one cell per row AND column, and everything a react view has of a property means
    // one ROW: its name, in `data` and on the controller, its descriptor, read at one key while the platform's own
    // renderer gives its caption up to it, and a platform-drawn one's per-row renderer, keyed by the row - under a
    // row-and-column key a value would be written under one key and read under the other, every edit lost without a
    // word. Where the projection CARRIES the property, that is not a runtime surprise to
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
                    + " Draw it as an ordinary property, or keep it out of what a React view projects");
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