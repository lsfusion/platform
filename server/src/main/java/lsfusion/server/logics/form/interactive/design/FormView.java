package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashSet;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.event.*;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.SIDHandler;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;
import lsfusion.server.base.version.interfaces.NFOrderMap;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.base.version.interfaces.NFSet;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerCustomSerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.GroupObjectView;
import lsfusion.server.logics.form.interactive.design.object.ObjectView;
import lsfusion.server.logics.form.interactive.design.object.TreeGroupView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static java.util.Collections.synchronizedMap;
import static lsfusion.server.logics.form.interactive.design.object.GroupObjectContainerSet.*;

public class FormView extends IdentityObject implements ServerCustomSerializable {

    // нужен для того, чтобы генерировать уникальный идентификаторы объектам рисования, для передачи их клиенту
    public final IDGenerator idGenerator = new DefaultIDGenerator();

    public FormEntity entity;

    public String canonicalName = "";
    public String creationPath = "";
    public String path = "";

    public Integer overridePageWidth;

    public LocalizedString getCaption() {
        return mainContainer.caption;
    }

    public NFOrderSet<FormScheduler> formSchedulers;

    // список деревеьев
    private NFSet<TreeGroupView> treeGroups = NFFact.set();
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
    public Pair<ImOrderSet<PropertyDrawView>, ImList<Integer>> getNFPropertiesComplexOrderSet(Version version) {
        return properties.getNFComplexOrderSet(version);
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

    protected PropertyDrawView editButton;
    protected PropertyDrawView xlsButton;
    protected PropertyDrawView dropButton;
    protected PropertyDrawView refreshButton;
    protected PropertyDrawView applyButton;
    protected PropertyDrawView cancelButton;
    protected PropertyDrawView okButton;
    protected PropertyDrawView closeButton;

    protected transient Map<TreeGroupEntity, TreeGroupView> mtreeGroups = synchronizedMap(new HashMap<>());
    public TreeGroupView get(TreeGroupEntity treeGroup) { return mtreeGroups.get(treeGroup); }

    protected transient Map<GroupObjectEntity, GroupObjectView> mgroupObjects = synchronizedMap(new HashMap<>());
    public GroupObjectView get(GroupObjectEntity groupObject) { return mgroupObjects.get(groupObject); }

    protected transient Map<ObjectEntity, ObjectView> mobjects = synchronizedMap(new HashMap<>());
    public ObjectView get(ObjectEntity object) { return mobjects.get(object); }

    protected transient Map<PropertyDrawEntity, PropertyDrawView> mproperties = synchronizedMap(new HashMap<>());
    public PropertyDrawView get(PropertyDrawEntity property) { return mproperties.get(property); }
    
    protected transient Map<PropertyDrawEntity, FilterView> mfilters = synchronizedMap(new HashMap<>());
    public FilterView getFilter(PropertyDrawEntity property) { return mfilters.get(property); }

    protected transient Map<RegularFilterGroupEntity, RegularFilterGroupView> mfilterGroups = synchronizedMap(new HashMap<>());
    public RegularFilterGroupView get(RegularFilterGroupEntity filterGroup) { return mfilterGroups.get(filterGroup); }

    protected NFOrderSet<ImList<PropertyDrawView>> pivotColumns = NFFact.orderSet();
    public ImOrderSet<ImList<PropertyDrawView>> getPivotColumns() {
        return pivotColumns.getOrderSet();
    }

    protected NFOrderSet<ImList<PropertyDrawView>> pivotRows = NFFact.orderSet();
    public ImOrderSet<ImList<PropertyDrawView>> getPivotRows() {
        return pivotRows.getOrderSet();
    }

    protected NFOrderSet<PropertyDrawView> pivotMeasures = NFFact.orderSet();
    public ImOrderSet<PropertyDrawView> getPivotMeasures() {
        return pivotMeasures.getOrderSet();
    }

    public ComponentView findById(int id) {
        return mainContainer.findById(id);
    }

    public FormView(FormEntity entity, Version version) {
        super(0);

        idGenerator.idRegister(0);
        
        this.entity = entity;

        mainContainer = new ContainerView(idGenerator.idShift(), true);
        mainContainer.width = -3;
        mainContainer.height = -3;
        setComponentSID(mainContainer, getBoxContainerSID(), version);

        Pair<ImOrderSet<GroupObjectEntity>, ImList<Integer>> groups = entity.getNFGroupsComplexOrderSet(version);
        for (int i = 0, size = groups.first.size() ; i < size ; i++) {
            addGroupObjectBase(groups.first.get(i), ComplexLocation.LAST(groups.second.get(i)), version);
        }

        for (TreeGroupEntity treeGroup : entity.getNFTreeGroupsIt(version)) {
            addTreeGroupBase(treeGroup, version);
        }

        Pair<ImOrderSet<PropertyDrawEntity>, ImList<Integer>> properties = entity.getNFPropertyDrawsComplexOrderSet(version);
        for (int i = 0, size = properties.first.size() ; i < size ; i++) {
            PropertyDrawEntity property = properties.first.get(i);
            PropertyDrawView view = addPropertyDrawBase(property, ComplexLocation.LAST(properties.second.get(i)), version);
            view.caption = property.initCaption;
            String initImage = property.initImage;
            if(initImage != null)
                view.setImage(initImage);
        }

        for (RegularFilterGroupEntity filterGroup : entity.getNFRegularFilterGroupsListIt(version)) {
            addRegularFilterGroupBase(filterGroup, version);
        }

        for (PropertyDrawEntity propertyDrawEntity : entity.getUserFiltersIt(version)) {
            addFilter(propertyDrawEntity, version);
        }

        for (ImList<PropertyDrawEntity> pivotColumn : entity.getNFPivotColumnsListIt(version)) {
            addPivotColumn(pivotColumn, version);
        }

        for (ImList<PropertyDrawEntity> pivotRow : entity.getNFPivotRowsListIt(version)) {
            addPivotRow(pivotRow, version);
        }

        for (PropertyDrawEntity pivotMeasure : entity.getNFPivotMeasuresListIt(version)) {
            addPivotMeasure(pivotMeasure, version);
        }

        initButtons(version);
    }

    public void addFilter(PropertyDrawEntity filterProperty, Version version) {
        if (!mfilters.containsKey(filterProperty)) {
            GroupObjectEntity groupObjectEntity = filterProperty.getNFToDraw(entity, version);
            PropertyDrawView propertyDrawView = get(filterProperty);
            FilterView filterView;
            if (groupObjectEntity.isInTree()) {
                filterView = get(groupObjectEntity.treeGroup).addFilter(propertyDrawView, version);
            } else {
                filterView = get(groupObjectEntity).addFilter(propertyDrawView, version);
            }
            mfilters.put(filterProperty, filterView);
        }
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean descending, Version version) {
        defaultOrders.add(get(property), descending, version);
    }

    public void addPivotColumn(ImList<PropertyDrawEntity> column, Version version) {
        pivotColumns.add(getPropertyDrawViewList(column), version);
    }

    public void addPivotRow(ImList<PropertyDrawEntity> row, Version version) {
        pivotRows.add(getPropertyDrawViewList(row), version);
    }

    public void addPivotMeasure(PropertyDrawEntity measure, Version version) {
        pivotMeasures.add(get(measure), version);
    }

    private ImList<PropertyDrawView> getPropertyDrawViewList(ImList<PropertyDrawEntity> propertyDrawEntityList) {
        return propertyDrawEntityList.mapListValues((PropertyDrawEntity value) -> get(value));
    }

    private void addPropertyDrawView(PropertyDrawView property) {
        mproperties.put(property.entity, property);
    }

    private PropertyDrawView addPropertyDrawBase(PropertyDrawEntity property, ComplexLocation<PropertyDrawView> location, Version version) {
        PropertyDrawView propertyView = new PropertyDrawView(property);
        properties.add(propertyView, location, version);
        addPropertyDrawView(propertyView);

        //походу инициализируем порядки по умолчанию
        Boolean descending = entity.getNFDefaultOrder(property, version);
        if (descending != null) {
            defaultOrders.add(propertyView, descending, version);
        }

        return propertyView;
    }

    public PropertyDrawView addPropertyDraw(PropertyDrawEntity property, ComplexLocation<PropertyDrawView> location, Version version) {
        return addPropertyDrawBase(property, location, version);
    }

    private void addGroupObjectView(GroupObjectView groupObjectView, Version version) {
        mgroupObjects.put(groupObjectView.entity, groupObjectView);

        boolean isInTree = groupObjectView.entity.isInTree();

        if(!isInTree) { // правильнее вообще не создавать компоненты, но для этого потребуется более сложный рефакторинг, поэтому пока просто сделаем так чтобы к ним нельзя было обратиться
            setComponentSID(groupObjectView.getGrid(), getGridSID(groupObjectView), version);
            setComponentSID(groupObjectView.getToolbarSystem(), getToolbarSystemSID(groupObjectView), version);
            setComponentSID(groupObjectView.getFiltersContainer(), getFiltersContainerSID(groupObjectView), version);
            setComponentSID(groupObjectView.getFilterControls(), getFilterControlsComponentSID(groupObjectView), version);
            setComponentSID(groupObjectView.getCalculations(), getCalculationsSID(groupObjectView), version);
        }

        for (ObjectView object : groupObjectView)
            mobjects.put(object.entity, object);
    }
    
    public GroupObjectView addGroupObjectBase(GroupObjectEntity groupObject, ComplexLocation<GroupObjectEntity> location, Version version) {
        GroupObjectView groupObjectView = new GroupObjectView(idGenerator, groupObject);
        groupObjects.add(groupObjectView, location.map(this::get), version);
        addGroupObjectView(groupObjectView, version);
        return groupObjectView;    
    }

    private TreeGroupView addTreeGroupBase(TreeGroupEntity treeGroup, Version version) {
        return addTreeGroupBase(treeGroup, ComplexLocation.DEFAULT(), version);
    }

    public GroupObjectView addGroupObject(GroupObjectEntity groupObject, ComplexLocation<GroupObjectEntity> location, Version version) {
        return addGroupObjectBase(groupObject, location, version);
    }

    public TreeGroupView addTreeGroup(TreeGroupEntity treeGroup, ComplexLocation<GroupObjectEntity> location, Version version) {
        return addTreeGroupBase(treeGroup, location, version);
    }

    private void addTreeGroupView(TreeGroupView treeGroupView, Version version) {
        mtreeGroups.put(treeGroupView.entity, treeGroupView);
        setComponentSID(treeGroupView, getGridSID(treeGroupView), version);
        setComponentSID(treeGroupView.getToolbarSystem(), getToolbarSystemSID(treeGroupView), version);
        setComponentSID(treeGroupView.getFiltersContainer(), getFiltersContainerSID(treeGroupView), version);
        setComponentSID(treeGroupView.getFilterControls(), getFilterControlsComponentSID(treeGroupView), version);
    }

    private TreeGroupView addTreeGroupBase(TreeGroupEntity treeGroup, ComplexLocation<GroupObjectEntity> location, Version version) {
        TreeGroupView treeGroupView = new TreeGroupView(this, treeGroup, version);
        treeGroups.add(treeGroupView, version);
        addTreeGroupView(treeGroupView, version);
        return treeGroupView;
    }

    private void addRegularFilterGroupView(RegularFilterGroupView filterGroupView, Version version) {
        mfilterGroups.put(filterGroupView.entity, filterGroupView);
        setComponentSID(filterGroupView, getFilterGroupSID(filterGroupView.entity), version);
    }

    private RegularFilterGroupView addRegularFilterGroupBase(RegularFilterGroupEntity filterGroup, Version version) {
        RegularFilterGroupView filterGroupView = new RegularFilterGroupView(filterGroup, version);
        regularFilters.add(filterGroupView, version);
        addRegularFilterGroupView(filterGroupView, version);
        return filterGroupView;
    }

    public RegularFilterGroupView addRegularFilterGroup(RegularFilterGroupEntity filterGroupEntity, Version version) {
        return addRegularFilterGroupBase(filterGroupEntity, version);
    }
    
    public RegularFilterView addRegularFilter(RegularFilterGroupEntity filterGroup, RegularFilterEntity filter, Version version) {
        RegularFilterGroupView filterGroupView = get(filterGroup);
        return filterGroupView.addFilter(filter, version);
    }

    public void fillComponentMaps() {
        for (GroupObjectView group : getGroupObjectsIt()) {
            addGroupObjectView(group, Version.descriptor());
        }

        for (TreeGroupView treeGroup : getTreeGroupsIt()) {
            addTreeGroupView(treeGroup, Version.descriptor());
        }

        for (PropertyDrawView property : getPropertiesIt()) {
            addPropertyDrawView(property);
        }

        for (RegularFilterGroupView filterGroup : getRegularFiltersIt()) {
            addRegularFilterGroupView(filterGroup, Version.descriptor());
        }

        initButtons(Version.descriptor());
    }

    private void initButtons(Version version) {
        editButton = getNFProperty(entity.editActionPropertyDraw, version);
        refreshButton = getNFProperty(entity.refreshActionPropertyDraw, version);
        applyButton = getNFProperty(entity.applyActionPropertyDraw, version);
        cancelButton = getNFProperty(entity.cancelActionPropertyDraw, version);
        okButton = getNFProperty(entity.okActionPropertyDraw, version);
        closeButton = getNFProperty(entity.closeActionPropertyDraw, version);
        dropButton = getNFProperty(entity.dropActionPropertyDraw, version);
    }

    public ContainerView createContainer() {
        return createContainer(null);
    }

    public ContainerView createContainer(Version version) {
        return createContainer(null, version);
    }

    public ContainerView createContainer(LocalizedString caption, Version version) {
        return createContainer(caption, null, null, version);
    }

    public ContainerView createContainer(LocalizedString caption, String name, String sID, Version version) {
        return createContainer(caption, name, sID, version, null);
    }

    public ContainerView createContainer(LocalizedString caption, String name, String sID, Version version, DebugInfo.DebugPoint debugPoint) {
        ContainerView container = new ContainerView(idGenerator.idShift());
        container.setDebugPoint(debugPoint);

        container.caption = caption;
        container.name = name;

        container.setSID(sID);
        if (sID != null) {
            addComponentToMapping(container, version);
        }
        return container;
    }

    private final SIDHandler<ComponentView> componentSIDHandler = new SIDHandler<ComponentView>() {
        public boolean checkUnique() {
            return false;
        }

        protected String getSID(ComponentView component) {
            return component.getSID();
        }
    };
    
    public void addComponentToMapping(ComponentView container, Version version) {
        componentSIDHandler.store(container, version);
    }

    public void removeContainerFromMapping(ContainerView container, Version version) {
        componentSIDHandler.remove(container, version);
    }

    public ComponentView getComponentBySID(String sid, Version version) {
        return componentSIDHandler.find(sid, version);
    }

    public PropertyDrawView getEditButton() {
        return editButton;
    }

    public PropertyDrawView getXlsButton() {
        return xlsButton;
    }

    public PropertyDrawView getDropButton() {
        return dropButton;
    }

    public PropertyDrawView getRefreshButton() {
        return refreshButton;
    }

    public PropertyDrawView getApplyButton() {
        return applyButton;
    }

    public PropertyDrawView getCancelButton() {
        return cancelButton;
    }

    public PropertyDrawView getOkButton() {
        return okButton;
    }

    public PropertyDrawView getCloseButton() {
        return closeButton;
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

    public GroupObjectView getNFGroupObject(GroupObjectEntity entity, Version version) {
        if (entity == null) {
            return null;
        }
        for (GroupObjectView groupObject : getNFGroupObjectsIt(version))
            if (entity.equals(groupObject.entity))
                return groupObject;
        return null;
    }

    public ObjectView getObject(ObjectEntity entity) {
        if (entity == null) {
            return null;
        }
        for (GroupObjectView groupObject : getGroupObjectsIt())
            for(ObjectView object : groupObject)
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

    public void setFont(FontInfo font) {

        for (PropertyDrawView property : getPropertiesIt()) {
            setFont(property, font);
        }
    }

    public void setFont(FontInfo font, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(PropertyDrawView property, FontInfo font) {
        property.design.setFont(font);
    }

    public void setCaptionFont(FontInfo captionFont) {
        for (PropertyDrawView property : getPropertiesIt()) {
            setCaptionFont(property, captionFont);
        }
    }

    public void setCaptionFont(FontInfo captionFont, GroupObjectEntity groupObject) {
        for (PropertyDrawView property : getProperties(groupObject)) {
            setCaptionFont(property, captionFont);
        }
    }

    public void setCaptionFont(PropertyDrawView property, FontInfo captionFont) {
        property.design.setCaptionFont(captionFont);
    }

    @IdentityLazy
    public boolean hasHeaders(GroupObjectEntity entity) {
        for (PropertyDrawView property : getProperties(entity))
            if (property.entity.isList(FormView.this.entity) && !property.entity.ignoreHasHeaders && property.getDrawCaption() != null)
                return true;
        return false;
    }

    public void setBackground(PropertyDrawView property, Color background) {
        property.design.background = background;
    }

    public void setFocusable(boolean focusable, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(groupObject)) {
            setFocusable(propertyView, focusable);
        }
    }

    public void setFocusable(ObjectEntity objectEntity, boolean focusable) {
        for (PropertyDrawView property : getProperties(objectEntity.groupTo)) {
            setFocusable(property, focusable);
        }
    }

    public void setFocusable(PropertyDrawView property, boolean focusable) {
        property.focusable = focusable;
    }

    public void setChangeOnSingleClick(PropertyDrawView property, Boolean changeOnSingleClick) {
        property.changeOnSingleClick = changeOnSingleClick;
    }

    public void setCaption(LocalizedString caption) {
        this.mainContainer.setCaption(caption);
    }

    public void setImage(String imagePath) {
        this.mainContainer.setImage(imagePath, this);
    }

    public void setChangeKey(PropertyDrawView property, KeyStroke keyStroke) {
        property.changeKey = new InputBindingEvent(keyStroke != null ? new KeyInputEvent(keyStroke) : null, null);
    }
    public void setChangeMouse(PropertyDrawView property, String mouseStroke) {
        property.changeMouse = new InputBindingEvent(new MouseInputEvent(mouseStroke), null);
    }

    protected void setComponentSID(ContainerView container, String sid, Version version) {
        setComponentSID((ComponentView) container, sid, version);
    }
    protected void setComponentSID(BaseComponentView component, String sid, Version version) {
        setComponentSID((ComponentView) component, sid, version);
    }
    private void setComponentSID(ComponentView component, String sid, Version version) {
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

    private static String getBoxContainerSID() {
        return FormContainerSet.BOX_CONTAINER;
    }

    private static String getFilterGroupSID(RegularFilterGroupEntity entity) {
        return FILTERGROUP_COMPONENT + "(" + entity.getSID() + ")";
    }

    private static String getGridSID(PropertyGroupContainerView entity) {
        return GRID_COMPONENT + "(" + entity.getPropertyGroupContainerSID() + ")";
    }

    private static String getToolbarSystemSID(PropertyGroupContainerView entity) {
        return TOOLBAR_SYSTEM_COMPONENT + "(" + entity.getPropertyGroupContainerSID() + ")";
    }

    private static String getFiltersContainerSID(PropertyGroupContainerView entity) {
        return FILTERS_CONTAINER + "(" + entity.getPropertyGroupContainerSID() + ")";
    }
    
    private static String getFilterControlsComponentSID(PropertyGroupContainerView entity) {
        return FILTER_CONTROLS_COMPONENT + "(" + entity.getPropertyGroupContainerSID() + ")";
    }
    
    private static String getCalculationsSID(PropertyGroupContainerView entity) {
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

        serializePivot(pool, outStream, getPivotColumns());
        serializePivot(pool, outStream, getPivotRows());
        pool.serializeCollection(outStream, getPivotMeasures());

        pool.writeString(outStream, canonicalName);
        pool.writeString(outStream, creationPath);
        pool.writeString(outStream, path);
        pool.writeInt(outStream, overridePageWidth);
        serializeFormSchedulers(outStream, formSchedulers.getOrderSet());
        serializeAsyncExecMap(outStream, entity.getAsyncExecMap(pool.context), pool.context);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        assert false;
        mainContainer = pool.deserializeObject(inStream);
        treeGroups = NFFact.finalSet(pool.deserializeSet(inStream));
//        groupObjects = NFFact.finalOrderSet(pool.deserializeList(inStream));
//        properties = NFFact.finalOrderSet(pool.deserializeList(inStream));
//        regularFilters = NFFact.finalOrderSet(pool.deserializeList(inStream));

        int orderCount = inStream.readInt();
        MOrderExclMap<PropertyDrawView, Boolean> mDefaultOrders = MapFact.mOrderExclMap(orderCount);
        for (int i = 0; i < orderCount; i++) {
            PropertyDrawView order = pool.deserializeObject(inStream);
            mDefaultOrders.exclAdd(order, inStream.readBoolean());
        }
        defaultOrders = NFFact.finalOrderMap(mDefaultOrders.immutableOrder());

        canonicalName = pool.readString(inStream);
        creationPath = pool.readString(inStream);
        path = pool.readString(inStream);
        overridePageWidth = pool.readInt(inStream);

        entity = pool.context.entity;

        fillComponentMaps();
    }

    private void serializePivot(ServerSerializationPool pool, DataOutputStream outStream, ImOrderSet<ImList<PropertyDrawView>> list) throws IOException {
        int pivotColumnsSize = list.size();
        outStream.writeInt(pivotColumnsSize);
        for(ImList<PropertyDrawView> pivotColumn : list) {
            pool.serializeCollection(outStream, pivotColumn);
        }
    }

    private void serializeFormSchedulers(DataOutputStream outStream, ImOrderSet<FormScheduler> list) throws IOException {
        outStream.writeInt(list.size());
        for(FormScheduler formScheduler : list) {
            formScheduler.serialize(outStream);
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

        for(RegularFilterGroupView regularFilter : getRegularFiltersIt())
            regularFilter.finalizeAroundInit();
                
        mainContainer.finalizeAroundInit();
        componentSIDHandler.finalizeChanges();

        pivotColumns.finalizeChanges();
        pivotRows.finalizeChanges();
        pivotMeasures.finalizeChanges();

        for(ComponentView removedComponent : removedComponents)
            if(removedComponent.getContainer() == null)
                removedComponent.finalizeAroundInit();
    }


    public void prereadAutoIcons(ConnectionContext context) {
        for(PropertyDrawView property : getPropertiesIt())
            property.getImage(context);

        mainContainer.prereadAutoIcons(this, context);
    }

    protected transient Set<ComponentView> removedComponents = new ConcurrentIdentityWeakHashSet<>();

    // the problem is that if removed components are not put somewhere they are not finalized
    public void removeComponent(ComponentView component, Version version) {
        if(component instanceof PropertyDrawView) {
            ((PropertyDrawView) component).entity.remove = true;
        }
        removedComponents.add(component);
        component.removeFromParent(version);
    }
}