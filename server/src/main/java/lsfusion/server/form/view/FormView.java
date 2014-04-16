package lsfusion.server.form.view;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.identity.IDGenerator;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.FontInfo;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.layout.AbstractForm;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.*;
import lsfusion.server.logics.mutables.interfaces.NFOrderMap;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.property.group.AbstractNode;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class FormView implements ServerIdentitySerializable, AbstractForm<ContainerView, ComponentView> {

    // нужен для того, чтобы генерировать уникальный идентификаторы объектам рисования, для передачи их клиенту
    protected IDGenerator idGenerator;

    public KeyStroke keyStroke = null;

    public String caption = "";

    public Integer overridePageWidth;

    public int autoRefresh = 0;

    // список деревеьев
    public NFOrderSet<TreeGroupView> treeGroups = NFFact.orderSet();
    public Iterable<TreeGroupView> getTreeGroupsIt() {
        return treeGroups.getIt();
    }
    public ImOrderSet<TreeGroupView> getTreeGroupsList() {
        return treeGroups.getOrderSet();
    }
    public Iterable<TreeGroupView> getNFTreeGroupsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return treeGroups.getNFListIt(version);
    }

    // список групп
    public NFOrderSet<GroupObjectView> groupObjects = NFFact.orderSet();
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
    public NFOrderSet<PropertyDrawView> properties = NFFact.orderSet();
    public Iterable<PropertyDrawView> getPropertiesIt() {
        return properties.getIt();
    }
    public ImOrderSet<PropertyDrawView> getPropertiesList() {
        return properties.getOrderSet();
    }
    public Iterable<PropertyDrawView> getNFPropertiesIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return properties.getNFIt(version);
    }
    public Iterable<PropertyDrawView> getNFPropertiesListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return properties.getNFListIt(version);
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

    protected PropertyDrawView printButton;
    protected PropertyDrawView editButton;
    protected PropertyDrawView xlsButton;
    protected PropertyDrawView dropButton;
    protected PropertyDrawView refreshButton;
    protected PropertyDrawView applyButton;
    protected PropertyDrawView cancelButton;
    protected PropertyDrawView okButton;
    protected PropertyDrawView closeButton;

    protected transient Map<TreeGroupEntity, TreeGroupView> mtreeGroups = new HashMap<TreeGroupEntity, TreeGroupView>();
    public TreeGroupView get(TreeGroupEntity treeGroup) { return mtreeGroups.get(treeGroup); }

    protected transient Map<GroupObjectEntity, GroupObjectView> mgroupObjects = new HashMap<GroupObjectEntity, GroupObjectView>();
    public GroupObjectView get(GroupObjectEntity groupObject) { return mgroupObjects.get(groupObject); }

    protected transient Map<ObjectEntity, ObjectView> mobjects = new HashMap<ObjectEntity, ObjectView>();
    public ObjectView get(ObjectEntity object) { return mobjects.get(object); }

    protected transient Map<PropertyDrawEntity, PropertyDrawView> mproperties = new HashMap<PropertyDrawEntity, PropertyDrawView>();
    public PropertyDrawView get(PropertyDrawEntity property) { return mproperties.get(property); }

    protected transient Map<RegularFilterGroupEntity, RegularFilterGroupView> mfilters = new HashMap<RegularFilterGroupEntity, RegularFilterGroupView>();
    public RegularFilterGroupView get(RegularFilterGroupEntity filterGroup) { return mfilters.get(filterGroup); }

    public FormView() {
    }
    
    public ComponentView findById(int id) {
        return mainContainer.findById(id);
    }

    public FormEntity<?> entity;

    public FormView(FormEntity<?> entity, Version version) {
        this.entity = entity;
        this.idGenerator = entity.getIDGenerator();

        mainContainer = new ContainerView(idGenerator.idShift());
        setComponentSID(mainContainer, getMainContainerSID());

        for (GroupObjectEntity group : entity.getNFGroupsListIt(version)) {
            addGroupObjectBase(group, version);
        }

        for (TreeGroupEntity treeGroup : entity.getNFTreeGroupsListIt(version)) {
            addTreeGroupBase(treeGroup, version);
        }

        for (PropertyDrawEntity property : entity.getNFPropertyDrawsListIt(version)) {
            addPropertyDrawBase(property, version);
        }

        for (RegularFilterGroupEntity filterGroup : entity.getNFRegularFilterGroupsListIt(version)) {
            addRegularFilterGroupBase(filterGroup, version);
        }

        initButtons(version);
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean ascending, Version version) {
        defaultOrders.add(get(property), ascending, version);
    }

    private void addPropertyDrawView(PropertyDrawView property) {
        mproperties.put(property.entity, property);
    }

    private PropertyDrawView addPropertyDrawBase(PropertyDrawEntity property, Version version) {
        PropertyDrawView propertyView = new PropertyDrawView(property);
        properties.add(propertyView, version);
        addPropertyDrawView(propertyView);

        //походу инициализируем порядки по умолчанию
        Boolean ascending = entity.getNFDefaultOrder(property, version);
        if (ascending != null) {
            defaultOrders.add(propertyView, ascending, version);
        }

        return propertyView;
    }

    public PropertyDrawView addPropertyDraw(PropertyDrawEntity property, Version version) {
        return addPropertyDrawBase(property, version);
    }

    public void movePropertyDrawTo(PropertyDrawEntity property, PropertyDrawEntity newNeighbour, boolean isRightNeighbour, Version version) {
        PropertyDrawView propertyView = mproperties.get(property);
        PropertyDrawView neighbourView = mproperties.get(newNeighbour);
        assert propertyView != null && neighbourView != null;

        properties.move(propertyView, neighbourView, isRightNeighbour, version);
    }

    private void addGroupObjectView(GroupObjectView groupObjectView) {
        mgroupObjects.put(groupObjectView.entity, groupObjectView);
        setComponentSID(groupObjectView.getGrid(), getGridSID(groupObjectView.entity));
        setComponentSID(groupObjectView.getShowType(), getShowTypeSID(groupObjectView.entity));
        setComponentSID(groupObjectView.getToolbar(), getToolbarSID(groupObjectView.entity));
        setComponentSID(groupObjectView.getFilter(), getFilterSID(groupObjectView.entity));

        for (ObjectView object : groupObjectView) {
            mobjects.put(object.entity, object);
            setComponentSID(object.classChooser, getClassChooserSID(object.entity));
        }
    }

    private GroupObjectView addGroupObjectBase(GroupObjectEntity groupObject, Version version) {
        GroupObjectView groupObjectView = new GroupObjectView(idGenerator, groupObject);
        groupObjects.add(groupObjectView, version);
        addGroupObjectView(groupObjectView);
        return groupObjectView;
    }

    public GroupObjectView addGroupObject(GroupObjectEntity groupObject, Version version) {
        return addGroupObjectBase(groupObject, version);
    }

    public TreeGroupView addTreeGroup(TreeGroupEntity treeGroup, Version version) {
        return addTreeGroupBase(treeGroup, version);
    }

    private void addTreeGroupView(TreeGroupView treeGroupView) {
        mtreeGroups.put(treeGroupView.entity, treeGroupView);
        setComponentSID(treeGroupView, getTreeSID(treeGroupView.entity));
        setComponentSID(treeGroupView.getToolbar(), getToolbarSID(treeGroupView.entity));
        setComponentSID(treeGroupView.getFilter(), getFilterSID(treeGroupView.entity));
    }

    private TreeGroupView addTreeGroupBase(TreeGroupEntity treeGroup, Version version) {
        TreeGroupView treeGroupView = new TreeGroupView(this, treeGroup, version);
        treeGroups.add(treeGroupView, version);
        addTreeGroupView(treeGroupView);
        return treeGroupView;
    }

    private void addRegularFilterGroupView(RegularFilterGroupView filterGroupView) {
        mfilters.put(filterGroupView.entity, filterGroupView);
        setComponentSID(filterGroupView, getRegularFilterGroupSID(filterGroupView.entity));
    }

    private RegularFilterGroupView addRegularFilterGroupBase(RegularFilterGroupEntity filterGroup, Version version) {
        RegularFilterGroupView filterGroupView = new RegularFilterGroupView(filterGroup);
        regularFilters.add(filterGroupView, version);
        addRegularFilterGroupView(filterGroupView);
        return filterGroupView;
    }

    public RegularFilterGroupView addRegularFilterGroup(RegularFilterGroupEntity filterGroupEntity, Version version) {
        return addRegularFilterGroupBase(filterGroupEntity, version);
    }

    public void fillComponentMaps() {
        for (GroupObjectView group : getGroupObjectsIt()) {
            addGroupObjectView(group);
        }

        for (TreeGroupView treeGroup : getTreeGroupsIt()) {
            addTreeGroupView(treeGroup);
        }

        for (PropertyDrawView property : getPropertiesIt()) {
            addPropertyDrawView(property);
        }

        for (RegularFilterGroupView filterGroup : getRegularFiltersIt()) {
            addRegularFilterGroupView(filterGroup);
        }

        initButtons(Version.DESCRIPTOR);
    }

    private void initButtons(Version version) {
        printButton = setupFormButton(entity.printActionPropertyDraw, "print", version);
        editButton = setupFormButton(entity.editActionPropertyDraw, "edit", version);
        xlsButton = setupFormButton(entity.xlsActionPropertyDraw, "xls", version);
        refreshButton = setupFormButton(entity.refreshActionPropertyDraw, "refresh", version);
        applyButton = setupFormButton(entity.applyActionPropertyDraw, "apply", version);
        cancelButton = setupFormButton(entity.cancelActionPropertyDraw, "cancel", version);
        okButton = setupFormButton(entity.okActionPropertyDraw, "ok", version);
        closeButton = setupFormButton(entity.closeActionPropertyDraw, "close", version);
        dropButton = setupFormButton(entity.dropActionPropertyDraw, "drop", version);
    }

    public int getID() {
        return entity.getID();
    }

    int ID;
    public void setID(int ID) {
        this.ID = ID;
    }

    public ContainerView createContainer() {
        return createContainer(null);
    }

    public ContainerView createContainer(String caption) {
        return createContainer(caption, null, null);
    }

    public ContainerView createContainer(String caption, String description, String sID) {
        ContainerView container = new ContainerView(idGenerator.idShift());
        container.setCaption(caption);
        container.setDescription(description);
        container.setSID(sID);
        if (sID != null) {
            addContainerToMapping(container);
        }
        return container;
    }

    private final SIDHandler<ComponentView> componentSIDHandler = new SIDHandler<ComponentView>() {
        public boolean checkUnique() {
            return false;
        }

        protected String getSID(ComponentView component) {
            return component.getSID();
        }};
    public void addContainerToMapping(ComponentView container) {
        componentSIDHandler.store(container);
    }

    public void removeContainerFromMapping(ContainerView container) {
        componentSIDHandler.remove(container);
    }

    public ComponentView getComponentBySID(String sid) {
        return componentSIDHandler.find(sid);
    }

    public PropertyDrawView getPrintButton() {
        return printButton;
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

    public List<PropertyDrawView> getProperties(AbstractNode group) {

        return getProperties(group, null);
    }

    public List<PropertyDrawView> getProperties(AbstractNode group, GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : getPropertiesList()) {
            if ((groupObject==null || groupObject.equals(property.entity.getToDraw(entity))) && group.hasChild(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawView> getProperties(Property prop, GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : getPropertiesList()) {
            if (groupObject.equals(property.entity.getToDraw(entity)) && prop.equals(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawView> getProperties(Property prop) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : getPropertiesIt()) {
            if (prop.equals(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawView> getProperties(GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

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

    public void setFont(AbstractGroup group, FontInfo font) {

        for (PropertyDrawView property : getProperties(group)) {
            setFont(property, font);
        }
    }

    public void setFont(AbstractGroup group, FontInfo font, GroupObjectEntity groupObject) {
        
        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(FontInfo font, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(LP property, FontInfo font, GroupObjectEntity groupObject) {
        setFont(property.property, font, groupObject);
    }

    public void setFont(Property property, FontInfo font, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(property, groupObject)) {
            setFont(propertyView, font);
        }
    }

    public void setFont(LP property, FontInfo font) {
        setFont(property.property, font);
    }

    public void setFont(Property property, FontInfo font) {

        for (PropertyDrawView propertyView : getProperties(property)) {
            setFont(propertyView, font);
        }
    }

    public void setFont(PropertyDrawView property, FontInfo font) {
        property.design.setFont(font);
    }

    public void setHeaderFont(FontInfo headerFont) {
        for (PropertyDrawView property : getPropertiesIt()) {
            setHeaderFont(property, headerFont);
        }
    }

    public void setHeaderFont(FontInfo headerFont, GroupObjectEntity groupObject) {
        for (PropertyDrawView property : getProperties(groupObject)) {
            setHeaderFont(property, headerFont);
        }
    }

    public void setHeaderFont(PropertyDrawView property, FontInfo headerFont) {
        property.design.setHeaderFont(headerFont);
    }

    public void setBackground(AbstractGroup group, Color background, GroupObjectEntity groupObject) {
        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setBackground(property, background);
        }
    }

    public void setBackground(LP prop, Color background) {
        setBackground(prop.property, background);
    }

    public void setBackground(Color background, GroupObjectEntity groupObject, LP... props) {
        for(LP prop : props)
            setBackground(prop.property, groupObject, background);
    }

    public void setBackground(Property prop, GroupObjectEntity groupObject, Color background) {

        for (PropertyDrawView property : getProperties(prop, groupObject)) {
            setBackground(property, background);
        }
    }

    public void setBackground(Property prop, Color background) {

        for (PropertyDrawView property : getProperties(prop)) {
            setBackground(property, background);
        }
    }

    public void setBackground(PropertyDrawView property, Color background) {
        property.design.background = background;
    }

    public void setFocusable(AbstractGroup group, boolean focusable, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setFocusable(property, focusable);
        }
    }

    public void setFocusable(LP property, boolean focusable) {
        setFocusable(property.property, focusable);
    }

    public void setFocusable(LP property, boolean focusable, GroupObjectEntity groupObject) {
        setFocusable(property.property, focusable, groupObject);
    }

    public void setFocusable(Property property, boolean focusable) {

        for (PropertyDrawView propertyView : getProperties(property)) {
            setFocusable(propertyView, focusable);
        }
    }

    public void setFocusable(Property property, boolean focusable, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(property, groupObject)) {
            setFocusable(propertyView, focusable);
        }
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

    public void setEditOnSingleClick(Boolean editOnSingleClick, Type type) {

        for (PropertyDrawView propertyView : getPropertiesIt()) {
            if (propertyView.entity.propertyObject.property.getType().equals(type))
                setEditOnSingleClick(propertyView, editOnSingleClick);
        }
    }

    public void setEditOnSingleClick(Boolean editOnSingleClick, GroupObjectEntity groupObject, Type type) {

        for (PropertyDrawView propertyView : getProperties(groupObject)) {
            if (propertyView.entity.propertyObject.property.getType().equals(type))
                setEditOnSingleClick(propertyView, editOnSingleClick);
        }
    }

    public void setEditOnSingleClick(PropertyDrawView property, Boolean editOnSingleClick) {
        property.editOnSingleClick = editOnSingleClick;
    }

    public void setKeyStroke(KeyStroke keyStroke) {
        this.keyStroke = keyStroke;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setEnabled(AbstractGroup group, boolean enabled, GroupObjectEntity groupObject) {
        setFocusable(group, enabled, groupObject);
        entity.setEditType(group, PropertyEditType.getReadonlyType(!enabled), groupObject);
    }

    public void setEnabled(LP property, boolean enabled) {
        setFocusable(property, enabled);
        entity.setEditType(property, PropertyEditType.getReadonlyType(!enabled));
    }

    public void setEnabled(LP property, boolean enabled, GroupObjectEntity groupObject) {
        setFocusable(property, enabled, groupObject);
        entity.setEditType(property, PropertyEditType.getReadonlyType(!enabled), groupObject);
    }

    public void setEnabled(Property property, boolean enabled) {
        setFocusable(property, enabled);
        entity.setEditType(property, PropertyEditType.getReadonlyType(!enabled));
    }

    public void setEnabled(Property property, boolean enabled, GroupObjectEntity groupObject) {
        setFocusable(property, enabled, groupObject);
        entity.setEditType(property, PropertyEditType.getReadonlyType(!enabled), groupObject);
    }

    public void setEnabled(boolean enabled, GroupObjectEntity groupObject) {
        setFocusable(enabled, groupObject);
        entity.setEditType(PropertyEditType.getReadonlyType(!enabled), groupObject);
    }

    public void setEnabled(ObjectEntity objectEntity, boolean enabled) {
        setFocusable(objectEntity, enabled);
        entity.setEditType(objectEntity, PropertyEditType.getReadonlyType(!enabled));
    }

    public void setEnabled(PropertyDrawView property, boolean enabled) {
        setFocusable(property, enabled);
        entity.setEditType(property.entity, PropertyEditType.getReadonlyType(!enabled));
    }

    public void setEditKey(LP property, KeyStroke keyStroke, GroupObjectEntity groupObject) {
        setEditKey(property.property, keyStroke, groupObject);
    }

    public void setEditKey(LP property, KeyStroke keyStroke) {
        setEditKey(property.property, keyStroke);
    }

    public void setEditKey(Property property, KeyStroke keyStroke, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(property, groupObject)) {
            setEditKey(propertyView, keyStroke);
        }
    }

    public void setEditKey(Property property, KeyStroke keyStroke) {

        for (PropertyDrawView propertyView : getProperties(property)) {
            setEditKey(propertyView, keyStroke);
        }
    }

    public void setEditKey(PropertyDrawView property, KeyStroke keyStroke) {
        property.editKey = keyStroke;
    }

    public void setPanelLabelAbove(AbstractGroup group, boolean panelLabelAbove, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setPanelLabelAbove(property, panelLabelAbove);
        }
    }

    public void setPanelLabelAbove(AbstractGroup group, boolean panelLabelAbove) {

        for (PropertyDrawView property : getProperties(group)) {
            setPanelLabelAbove(property, panelLabelAbove);
        }
    }

    public void setPanelLabelAbove(PropertyDrawView property, boolean panelLabelAbove) {
        property.panelLabelAbove = panelLabelAbove;
    }

    public void setPreferredSize(AbstractGroup group, Dimension size) {
        for (PropertyDrawView property : getProperties(group)) {
            setPreferredSize(property, size);
        }
    }

    private void setPreferredSize(PropertyDrawView property, Dimension size) {
        property.preferredSize = new Dimension(size);
    }

    public void setPropertyDrawViewHide(boolean hide, PropertyDrawEntity... properties) {
        for (PropertyDrawEntity property : properties) {
            setPropertyDrawViewHide(property, hide);
        }
    }

    public void setPropertyDrawViewHide(PropertyDrawEntity property, boolean hide) {
        getProperty(property).hide = hide;
    }

    protected void setComponentSID(ComponentView component, String sid) {
        component.setSID(sid);
        addContainerToMapping(component);
    }

    public ContainerView getContainerBySID(String sid) {
        ComponentView component = getComponentBySID(sid);
        if (component != null && !(component instanceof ContainerView)) {
            throw new IllegalStateException(sid + " component has to be container");
        }
        return (ContainerView) component;
    }

    private PropertyDrawView setupFormButton(PropertyDrawEntity function, String type, Version version) {
        PropertyDrawView functionView = getNFProperty(function, version);        
        setComponentSID(functionView, getClientFunctionSID(type));
        return functionView;         
    }

    private static String getMainContainerSID() {
        return "main";
    }

    private static String getTreeSID(TreeGroupEntity entity) {
        return entity.getSID() + ".tree";
    }

    private static String getRegularFilterGroupSID(RegularFilterGroupEntity entity) {
        return "filters." + entity.getSID();
    }

    private static String getGridSID(GroupObjectEntity entity) {
        return entity.getSID() + ".grid";
    }

    private static String getToolbarSID(IdentityObject entity) {
        return entity.getSID() + ".toolbar";
    }

    private static String getFilterSID(IdentityObject entity) {
        return entity.getSID() + ".filter";
    }

    private static String getShowTypeSID(GroupObjectEntity entity) {
        return entity.getSID() + ".showType";
    }

    private static String getClassChooserSID(ObjectEntity entity) {
        return entity.getSID() + ".classChooser";
    }

    private static String getClientFunctionSID(String type) {
        return "functions." + type;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, mainContainer, serializationType);
        pool.serializeCollection(outStream, getTreeGroupsList(), serializationType);
        pool.serializeCollection(outStream, getGroupObjectsListIt(), serializationType);
        pool.serializeCollection(outStream, getPropertiesList(), serializationType);
        pool.serializeCollection(outStream, getRegularFiltersList());

        ImOrderMap<PropertyDrawView, Boolean> defaultOrders = getDefaultOrders();
        int size = defaultOrders.size();
        outStream.writeInt(size);
        for (int i=0;i<size;i++) {
            pool.serializeObject(outStream, defaultOrders.getKey(i), serializationType);
            outStream.writeBoolean(defaultOrders.getValue(i));
        }

        pool.writeObject(outStream, keyStroke);
        pool.writeString(outStream, caption);
        pool.writeInt(outStream, overridePageWidth);
        outStream.writeInt(autoRefresh);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        mainContainer = pool.deserializeObject(inStream);
        treeGroups = NFFact.finalOrderSet(pool.<TreeGroupView>deserializeList(inStream));
        groupObjects = NFFact.finalOrderSet(pool.<GroupObjectView>deserializeList(inStream));
        properties = NFFact.finalOrderSet(pool.<PropertyDrawView>deserializeList(inStream));
        regularFilters = NFFact.finalOrderSet(pool.<RegularFilterGroupView>deserializeList(inStream));

        int orderCount = inStream.readInt();
        MOrderExclMap<PropertyDrawView, Boolean> mDefaultOrders = MapFact.mOrderExclMap(orderCount);
        for (int i = 0; i < orderCount; i++) {
            PropertyDrawView order = pool.deserializeObject(inStream);
            mDefaultOrders.exclAdd(order, inStream.readBoolean());
        }
        defaultOrders = NFFact.finalOrderMap(mDefaultOrders.immutableOrder());

        keyStroke = pool.readObject(inStream);
        caption = pool.readString(inStream);
        overridePageWidth = pool.readInt(inStream);
        autoRefresh = inStream.readInt();

        entity = pool.context.entity;

        fillComponentMaps();
    }
}