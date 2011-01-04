package platform.server.form.view;

import platform.base.OrderedMap;
import platform.base.identity.IDGenerator;
import platform.interop.form.layout.AbstractForm;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.form.entity.*;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FormView implements ServerIdentitySerializable, AbstractForm<ContainerView, ComponentView, FunctionView> {

    // нужен для того, чтобы генерировать уникальный идентификаторы объектам рисования, для передачи их клиенту
    protected IDGenerator idGenerator;

    public Integer overridePageWidth;

    public ContainerView mainContainer;

    public void setMainContainer(ContainerView mainContainer) {
        this.mainContainer = mainContainer;
    }

    // список деревеьев
    public List<TreeGroupView> treeGroups = new ArrayList<TreeGroupView>();

    // список групп
    public List<GroupObjectView> groupObjects = new ArrayList<GroupObjectView>();

    // список свойств
    public List<PropertyDrawView> properties = new ArrayList<PropertyDrawView>();

    // список фильтров
    public List<RegularFilterGroupView> regularFilters = new ArrayList<RegularFilterGroupView>();

    public OrderedMap<PropertyDrawView,Boolean> defaultOrders = new OrderedMap<PropertyDrawView, Boolean>();

    private FunctionView printFunction;
    private FunctionView xlsFunction;
    private FunctionView nullFunction;
    private FunctionView refreshFunction;
    private FunctionView applyFunction;
    private FunctionView cancelFunction;
    private FunctionView okFunction;
    private FunctionView closeFunction;

    public FunctionView getPrintFunction() {
        return printFunction;
    }

    public FunctionView getXlsFunction() {
        return xlsFunction;
    }

    public FunctionView getNullFunction() {
        return nullFunction;
    }

    public FunctionView getRefreshFunction() {
        return refreshFunction;
    }

    public FunctionView getApplyFunction() {
        return applyFunction;
    }

    public FunctionView getCancelFunction() {
        return cancelFunction;
    }

    public FunctionView getOkFunction() {
        return okFunction;
    }

    public FunctionView getCloseFunction() {
        return closeFunction;
    }

    public void setPrintFunction(FunctionView printFunction) {
        this.printFunction = printFunction;
    }

    public void setXlsFunction(FunctionView xlsFunction) {
        this.xlsFunction = xlsFunction;
    }

    public void setNullFunction(FunctionView nullFunction) {
        this.nullFunction = nullFunction;
    }

    public void setRefreshFunction(FunctionView refreshFunction) {
        this.refreshFunction = refreshFunction;
    }

    public void setApplyFunction(FunctionView applyFunction) {
        this.applyFunction = applyFunction;
    }

    public void setCancelFunction(FunctionView cancelFunction) {
        this.cancelFunction = cancelFunction;
    }

    public void setOkFunction(FunctionView okFunction) {
        this.okFunction = okFunction;
    }

    public void setCloseFunction(FunctionView closeFunction) {
        this.closeFunction = closeFunction;
    }

    public List<PropertyDrawView> order = new ArrayList<PropertyDrawView>();

    public boolean readOnly = false;

    public KeyStroke keyStroke = null;

    public String caption = "";

    public FormView() {
    }

    public FormEntity<?> entity;

    public FormView(FormEntity<?> entity) {

        this.entity = entity;
        this.idGenerator = entity.getIDGenerator();
    }

    public int getID() {
        return entity.getID();
    }

    int ID;
    public void setID(int ID) {
        this.ID = ID;
    }

    public ContainerView addContainer() {
        return addContainer(null);
    }

    public ContainerView addContainer(String title) {

        ContainerView container = new ContainerView(idGenerator.idShift());
        container.title = title;
        return container;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeBoolean(readOnly);
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

        pool.serializeObject(outStream, printFunction);
        pool.serializeObject(outStream, xlsFunction);
        pool.serializeObject(outStream, nullFunction);
        pool.serializeObject(outStream, refreshFunction);
        pool.serializeObject(outStream, applyFunction);
        pool.serializeObject(outStream, cancelFunction);
        pool.serializeObject(outStream, okFunction);
        pool.serializeObject(outStream, closeFunction);

        pool.serializeCollection(outStream, order, serializationType);

        pool.writeObject(outStream, keyStroke);
        pool.writeString(outStream, caption);
        pool.writeInt(outStream, overridePageWidth);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        readOnly = inStream.readBoolean();

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

        printFunction = pool.deserializeObject(inStream);
        xlsFunction = pool.deserializeObject(inStream);
        nullFunction = pool.deserializeObject(inStream);
        refreshFunction = pool.deserializeObject(inStream);
        applyFunction = pool.deserializeObject(inStream);
        cancelFunction = pool.deserializeObject(inStream);
        okFunction = pool.deserializeObject(inStream);
        closeFunction = pool.deserializeObject(inStream);

        order = pool.deserializeList(inStream);

        keyStroke = pool.readObject(inStream);
        caption = pool.readString(inStream);
        overridePageWidth = pool.readInt(inStream);

        entity = pool.context.entity;
    }

    public void addIntersection(ComponentView comp1, ComponentView comp2, DoNotIntersectSimplexConstraint cons) {

        if (comp1.container != comp2.container)
            throw new RuntimeException("Запрещено создавать пересечения для объектов в разных контейнерах");
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

    public void setFont(Font font) {

        for (PropertyDrawView property : getProperties()) {
            setFont(property, font);
        }
    }

    public void setFont(AbstractGroup group, Font font) {

        for (PropertyDrawView property : getProperties(group)) {
            setFont(property, font);
        }
    }

    public void setFont(AbstractGroup group, Font font, GroupObjectEntity groupObject) {
        
        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(Font font, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(LP property, Font font, GroupObjectEntity groupObject) {
        setFont(property.property, font, groupObject);
    }

    public void setFont(Property property, Font font, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(property, groupObject)) {
            setFont(propertyView, font);
        }
    }

    public void setFont(LP property, Font font) {
        setFont(property.property, font);
    }

    public void setFont(Property property, Font font) {

        for (PropertyDrawView propertyView : getProperties(property)) {
            setFont(propertyView, font);
        }
    }

    public void setFont(PropertyDrawView property, Font font) {
        property.design.font = font;
    }


    public void setHeaderFont(Font font) {
        for (PropertyDrawView property : getProperties()) {
            setHeaderFont(property, font);
        }
    }

    public void setHeaderFont(Font font, GroupObjectEntity groupObject) {
        for (PropertyDrawView property : getProperties(groupObject)) {
            setHeaderFont(property, font);
        }
    }

    public void setHeaderFont(PropertyDrawView property, Font font) {
        property.design.headerFont = font;
    }

    public void setBackground(AbstractGroup group, Color background, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setBackground(property, background);
        }
    }

    public void setBackground(LP prop, Color background) {
        setBackground(prop.property, background);
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

    public void setReadOnly(AbstractGroup group, boolean readOnly, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setReadOnly(property, readOnly);
        }
    }

    public void setReadOnly(LP property, boolean readOnly) {
        setReadOnly(property.property, readOnly);
    }

    public void setReadOnly(LP property, boolean readOnly, GroupObjectEntity groupObject) {
        setReadOnly(property.property, readOnly, groupObject);
    }

    public void setReadOnly(Property property, boolean readOnly) {

        for (PropertyDrawView propertyView : getProperties(property)) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(Property property, boolean readOnly, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(property, groupObject)) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(boolean readOnly, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(groupObject)) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(ObjectEntity objectEntity, boolean readOnly) {
        for (PropertyDrawView property : getProperties(objectEntity.groupTo)) {
            setReadOnly(property, readOnly);
        }
    }

    public void setReadOnly(PropertyDrawView property, boolean readOnly) {
        property.readOnly = readOnly;
    }

    public void setKeyStroke(KeyStroke keyStroke) {
        this.keyStroke = keyStroke;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setEnabled(AbstractGroup group, boolean readOnly, GroupObjectEntity groupObject) {
        setFocusable(group, readOnly, groupObject);
        setReadOnly(group, !readOnly, groupObject);
    }

    public void setEnabled(LP property, boolean readOnly) {
        setFocusable(property, readOnly);
        setReadOnly(property, !readOnly);
    }

    public void setEnabled(LP property, boolean readOnly, GroupObjectEntity groupObject) {
        setFocusable(property, readOnly, groupObject);
        setReadOnly(property, !readOnly, groupObject);
    }

    public void setEnabled(Property property, boolean readOnly) {
        setFocusable(property, readOnly);
        setReadOnly(property, !readOnly);
    }

    public void setEnabled(Property property, boolean readOnly, GroupObjectEntity groupObject) {
        setFocusable(property, readOnly, groupObject);
        setReadOnly(property, !readOnly, groupObject);
    }

    public void setEnabled(boolean readOnly, GroupObjectEntity groupObject) {
        setFocusable(readOnly, groupObject);
        setReadOnly(!readOnly, groupObject);
    }

    public void setEnabled(ObjectEntity objectEntity, boolean readOnly) {
        setFocusable(objectEntity, readOnly);
        setReadOnly(objectEntity, !readOnly);
    }

    public void setEnabled(PropertyDrawView property, boolean readOnly) {
        setFocusable(property, readOnly);
        setReadOnly(property, !readOnly);
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

    public void setConstraintsFillHorizontal(AbstractGroup group, double fillFactor) {
        for (PropertyDrawView property : getProperties(group)) {
            setConstraintsFillHorizontal(property, fillFactor);
        }
    }

    private void setConstraintsFillHorizontal(PropertyDrawView property, double fillFactor) {
        property.constraints.fillHorizontal = fillFactor;
    }

    public void setPreferredSize(AbstractGroup group, Dimension size) {
        for (PropertyDrawView property : getProperties(group)) {
            setPreferredSize(property, size);
        }
    }

    private void setPreferredSize(PropertyDrawView property, Dimension size) {
        property.preferredSize = new Dimension(size);
    }
}