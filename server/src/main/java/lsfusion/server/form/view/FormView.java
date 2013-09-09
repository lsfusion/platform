package lsfusion.server.form.view;

import lsfusion.base.OrderedMap;
import lsfusion.base.identity.IDGenerator;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.FontInfo;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.layout.AbstractForm;
import lsfusion.interop.form.layout.DoNotIntersectSimplexConstraint;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.linear.LP;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormView implements ServerIdentitySerializable, AbstractForm<ContainerView, ComponentView> {

    // нужен для того, чтобы генерировать уникальный идентификаторы объектам рисования, для передачи их клиенту
    protected IDGenerator idGenerator;

    private final Map<String, ComponentView> sidToComponent = new HashMap<String, ComponentView>();

    public KeyStroke keyStroke = null;

    public String caption = "";

    public Integer overridePageWidth;

    public int autoRefresh = 0;

    // список деревеьев
    public List<TreeGroupView> treeGroups = new ArrayList<TreeGroupView>();

    // список групп
    public List<GroupObjectView> groupObjects = new ArrayList<GroupObjectView>();

    // список свойств
    public List<PropertyDrawView> properties = new ArrayList<PropertyDrawView>();

    // список фильтров
    public List<RegularFilterGroupView> regularFilters = new ArrayList<RegularFilterGroupView>();

    protected OrderedMap<PropertyDrawView,Boolean> defaultOrders = new OrderedMap<PropertyDrawView, Boolean>();

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

    public FormView(FormEntity<?> entity) {
        this.entity = entity;
        this.idGenerator = entity.getIDGenerator();

        mainContainer = new ContainerView(idGenerator.idShift());
        setComponentSID(mainContainer, getMainContainerSID());

        for (GroupObjectEntity group : entity.groups) {
            addGroupObjectBase(group);
        }

        for (TreeGroupEntity treeGroup : entity.treeGroups) {
            addTreeGroupBase(treeGroup);
        }

        for (PropertyDrawEntity property : entity.propertyDraws) {
            addPropertyDrawBase(property);
        }

        for (RegularFilterGroupEntity filterGroup : entity.regularFilterGroups) {
            addRegularFilterGroupBase(filterGroup);
        }

        initButtons();
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean ascending) {
        defaultOrders.put(get(property), ascending);
    }

    private void addPropertyDrawView(PropertyDrawView property) {
        mproperties.put(property.entity, property);
    }

    private PropertyDrawView addPropertyDrawBase(PropertyDrawEntity property) {
        PropertyDrawView propertyView = new PropertyDrawView(property);
        properties.add(propertyView);
        addPropertyDrawView(propertyView);

        //походу инициализируем порядки по умолчанию
        Boolean ascending = entity.defaultOrders.get(property);
        if (ascending != null) {
            defaultOrders.put(propertyView, ascending);
        }

        return propertyView;
    }

    public PropertyDrawView addPropertyDraw(PropertyDrawEntity property) {
        return addPropertyDrawBase(property);
    }

    public void movePropertyDrawTo(PropertyDrawEntity property, PropertyDrawEntity newNeighbour, boolean isRightNeighbour) {
        PropertyDrawView propertyView = mproperties.get(property);
        PropertyDrawView neighbourView = mproperties.get(newNeighbour);
        assert propertyView != null && neighbourView != null;

        properties.remove(propertyView);
        int neighbourIndex = properties.indexOf(neighbourView);
        if (isRightNeighbour) {
            ++neighbourIndex;
        }
        properties.add(neighbourIndex, propertyView);
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

    private GroupObjectView addGroupObjectBase(GroupObjectEntity groupObject) {
        GroupObjectView groupObjectView = new GroupObjectView(idGenerator, groupObject);
        groupObjects.add(groupObjectView);
        addGroupObjectView(groupObjectView);
        return groupObjectView;
    }

    public GroupObjectView addGroupObject(GroupObjectEntity groupObject) {
        return addGroupObjectBase(groupObject);
    }

    public TreeGroupView addTreeGroup(TreeGroupEntity treeGroup) {
        return addTreeGroupBase(treeGroup);
    }

    private void addTreeGroupView(TreeGroupView treeGroupView) {
        mtreeGroups.put(treeGroupView.entity, treeGroupView);
        setComponentSID(treeGroupView, getTreeSID(treeGroupView.entity));
        setComponentSID(treeGroupView.getToolbar(), getToolbarSID(treeGroupView.entity));
        setComponentSID(treeGroupView.getFilter(), getFilterSID(treeGroupView.entity));
    }

    private TreeGroupView addTreeGroupBase(TreeGroupEntity treeGroup) {
        TreeGroupView treeGroupView = new TreeGroupView(this, treeGroup);
        treeGroups.add(treeGroupView);
        addTreeGroupView(treeGroupView);
        return treeGroupView;
    }

    private void addRegularFilterGroupView(RegularFilterGroupView filterGroupView) {
        mfilters.put(filterGroupView.entity, filterGroupView);
        setComponentSID(filterGroupView, getRegularFilterGroupSID(filterGroupView.entity));
    }

    private RegularFilterGroupView addRegularFilterGroupBase(RegularFilterGroupEntity filterGroup) {
        RegularFilterGroupView filterGroupView = new RegularFilterGroupView(filterGroup);
        regularFilters.add(filterGroupView);
        addRegularFilterGroupView(filterGroupView);
        return filterGroupView;
    }

    public RegularFilterGroupView addRegularFilterGroup(RegularFilterGroupEntity filterGroupEntity) {
        return addRegularFilterGroupBase(filterGroupEntity);
    }

    public void fillComponentMaps() {
        for (GroupObjectView group : groupObjects) {
            addGroupObjectView(group);
        }

        for (TreeGroupView treeGroup : treeGroups) {
            addTreeGroupView(treeGroup);
        }

        for (PropertyDrawView property : properties) {
            addPropertyDrawView(property);
        }

        for (RegularFilterGroupView filterGroup : regularFilters) {
            addRegularFilterGroupView(filterGroup);
        }

        initButtons();
    }

    private void initButtons() {
        printButton = setupFormButton(entity.printActionPropertyDraw, "print");
        editButton = setupFormButton(entity.editActionPropertyDraw, "edit");
        xlsButton = setupFormButton(entity.xlsActionPropertyDraw, "xls");
        refreshButton = setupFormButton(entity.refreshActionPropertyDraw, "refresh");
        applyButton = setupFormButton(entity.applyActionPropertyDraw, "apply");
        cancelButton = setupFormButton(entity.cancelActionPropertyDraw, "cancel");
        okButton = setupFormButton(entity.okActionPropertyDraw, "ok");
        closeButton = setupFormButton(entity.closeActionPropertyDraw, "close");
        dropButton = setupFormButton(entity.dropActionPropertyDraw, "drop");
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
            sidToComponent.put(sID, container);
        }
        return container;
    }

    public void removeContainerFromMapping(ContainerView container) {
        sidToComponent.remove(container.getSID());
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

    public void addIntersection(ComponentView comp1, ComponentView comp2, DoNotIntersectSimplexConstraint cons) {

        if (comp1.container != comp2.container)
            throw new RuntimeException(ServerResourceBundle.getString("form.view.forbidden.to.create.the.intersection.of.objects.in.different.containers"));
        comp1.constraints.intersects.put(comp2, cons);
    }

    public GroupObjectView getGroupObject(GroupObjectEntity entity) {
        if (entity == null) {
            return null;
        }
        for (GroupObjectView groupObject : groupObjects)
            if (entity.equals(groupObject.entity))
                return groupObject;
        return null;
    }

    public ObjectView getObject(ObjectEntity entity) {
        if (entity == null) {
            return null;
        }
        for (GroupObjectView groupObject : groupObjects)
            for(ObjectView object : groupObject)
                if (entity.equals(object.entity))
                    return object;
        return null;
    }

    public TreeGroupView getTreeGroup(TreeGroupEntity entity) {
        if (entity == null) {
            return null;
        }
        for (TreeGroupView treeGroup : treeGroups)
            if (entity.equals(treeGroup.entity))
                return treeGroup;
        return null;
    }

    public PropertyDrawView getProperty(PropertyDrawEntity entity) {
        if (entity == null) {
            return null;
        }
        for (PropertyDrawView property : properties) {
            if (entity.equals(property.entity)) {
                return property;
            }
        }
        return null;
    }

    public List<PropertyDrawView> getProperties() {
        return properties;
    }

    public List<PropertyDrawView> getProperties(AbstractNode group) {

        return getProperties(group, null);
    }

    public List<PropertyDrawView> getProperties(AbstractNode group, GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : properties) {
            if ((groupObject==null || groupObject.equals(property.entity.getToDraw(entity))) && group.hasChild(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawView> getProperties(Property prop, GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : properties) {
            if (groupObject.equals(property.entity.getToDraw(entity)) && prop.equals(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawView> getProperties(Property prop) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : properties) {
            if (prop.equals(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawView> getProperties(GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : properties) {
            if (groupObject.equals(property.entity.getToDraw(entity))) {
                result.add(property);
            }
        }

        return result;
    }

    public void setFont(FontInfo font) {

        for (PropertyDrawView property : getProperties()) {
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
        for (PropertyDrawView property : getProperties()) {
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

        for (PropertyDrawView propertyView : getProperties()) {
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
        sidToComponent.put(component.getSID(), component);
    }

    public ComponentView getComponentBySID(String sid) {
        return sidToComponent.get(sid);
    }

    public ContainerView getContainerBySID(String sid) {
        ComponentView component = getComponentBySID(sid);
        if (component != null && !(component instanceof ContainerView)) {
            throw new IllegalStateException(sid + " component has to be container");
        }
        return (ContainerView) component;
    }

    private PropertyDrawView setupFormButton(PropertyDrawEntity function, String type) {
        PropertyDrawView functionView = getProperty(function);        
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
        pool.serializeCollection(outStream, treeGroups, serializationType);
        pool.serializeCollection(outStream, groupObjects, serializationType);
        pool.serializeCollection(outStream, properties, serializationType);
        pool.serializeCollection(outStream, regularFilters);

        outStream.writeInt(defaultOrders.size());
        for (Map.Entry<PropertyDrawView, Boolean> entry : defaultOrders.entrySet()) {
            pool.serializeObject(outStream, entry.getKey(), serializationType);
            outStream.writeBoolean(entry.getValue());
        }

        pool.writeObject(outStream, keyStroke);
        pool.writeString(outStream, caption);
        pool.writeInt(outStream, overridePageWidth);
        outStream.writeInt(autoRefresh);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        mainContainer = pool.deserializeObject(inStream);
        treeGroups = pool.deserializeList(inStream);
        groupObjects = pool.deserializeList(inStream);
        properties = pool.deserializeList(inStream);
        regularFilters = pool.deserializeList(inStream);

        int orderCount = inStream.readInt();
        for (int i = 0; i < orderCount; i++) {
            PropertyDrawView order = pool.deserializeObject(inStream);
            defaultOrders.put(order, inStream.readBoolean());
        }

        keyStroke = pool.readObject(inStream);
        caption = pool.readString(inStream);
        overridePageWidth = pool.readInt(inStream);
        autoRefresh = inStream.readInt();

        entity = pool.context.entity;

        fillComponentMaps();
    }
}