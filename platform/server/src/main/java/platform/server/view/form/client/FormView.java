package platform.server.view.form.client;

import platform.base.OrderedMap;
import platform.base.IDGenerator;
import platform.base.DefaultIDGenerator;
import platform.server.view.navigator.CellViewNavigator;
import platform.server.view.navigator.PropertyViewNavigator;
import platform.server.view.navigator.GroupObjectNavigator;
import platform.server.view.form.GroupObjectImplement;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.Property;
import platform.server.logics.linear.LP;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.awt.*;

public class FormView implements ClientSerialize {

    // нужен для того, чтобы генерировать уникальный идентификаторы объектам рисования, для передачи их клиенту
    protected IDGenerator idGenerator = new DefaultIDGenerator();

    public Collection<ContainerView> containers = new ArrayList<ContainerView>();

    public ContainerView addContainer() {
        return addContainer(null);
    }

    public ContainerView addContainer(String title) {

        ContainerView container = new ContainerView(idGenerator.idShift());
        container.title = title;

        containers.add(container);
        return container;
    }

    // список групп
    public List<GroupObjectImplementView> groupObjects = new ArrayList<GroupObjectImplementView>();

    // список свойств
    public List<PropertyCellView> properties = new ArrayList<PropertyCellView>();

    // список фильтров
    public List<RegularFilterGroupView> regularFilters = new ArrayList<RegularFilterGroupView>();

    public OrderedMap<CellViewNavigator,Boolean> defaultOrders = new OrderedMap<CellViewNavigator, Boolean>();

    public FunctionView printView = new FunctionView(idGenerator.idShift());
    public FunctionView xlsView = new FunctionView(idGenerator.idShift());
    public FunctionView nullView = new FunctionView(idGenerator.idShift());
    public FunctionView refreshView = new FunctionView(idGenerator.idShift());
    public FunctionView applyView = new FunctionView(idGenerator.idShift());
    public FunctionView cancelView = new FunctionView(idGenerator.idShift());
    public FunctionView okView = new FunctionView(idGenerator.idShift());
    public FunctionView closeView = new FunctionView(idGenerator.idShift());

    public List<CellView> order = new ArrayList<CellView>();

    public boolean readOnly = false;

    public FormView() {
    }

    static <T extends ClientSerialize> void serializeList(DataOutputStream outStream, Collection<T> list) throws IOException {
        outStream.writeInt(list.size());
        for(T element : list)
            element.serialize(outStream);
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeBoolean(readOnly);

        List<ContainerView> orderedContainers = new ArrayList<ContainerView>();
        for(ContainerView container : containers)
            container.fillOrderList(orderedContainers);
        serializeList(outStream,orderedContainers);

        serializeList(outStream,groupObjects);
        serializeList(outStream, properties);
        serializeList(outStream,regularFilters);

        outStream.writeInt(defaultOrders.size());
        for(Map.Entry<CellViewNavigator,Boolean> order : defaultOrders.entrySet()) {
            outStream.writeBoolean(order.getKey() instanceof PropertyViewNavigator);
            outStream.writeInt(order.getKey().ID);
            outStream.writeBoolean(order.getValue());
        }

        printView.serialize(outStream);
        xlsView.serialize(outStream);
        nullView.serialize(outStream);
        refreshView.serialize(outStream);
        applyView.serialize(outStream);
        cancelView.serialize(outStream);
        okView.serialize(outStream);
        closeView.serialize(outStream);

        outStream.writeInt(order.size());
        for(CellView orderCell : order) {
            outStream.writeInt(orderCell.getID());
            if (orderCell instanceof PropertyCellView)
                outStream.writeBoolean(true);
            else {
                outStream.writeBoolean(false);
                outStream.writeBoolean(orderCell instanceof ClassCellView);
            }
        }
    }

    public void addIntersection(ComponentView comp1, ComponentView comp2, DoNotIntersectSimplexConstraint cons) {

        if (comp1.container != comp2.container)
            throw new RuntimeException("Запрещено создавать пересечения для объектов в разных контейнерах");
        comp1.constraints.intersects.put(comp2, cons);
    }

    public List<PropertyCellView> getProperties() {
        return properties;
    }

    public List<CellView> getCells() {

        List<CellView> result = new ArrayList<CellView>(getProperties());

        for (GroupObjectImplementView groupObject : groupObjects)
            for (ObjectImplementView object : groupObject) {
                result.add(object.objectCellView);
                result.add(object.classCellView);
            }

        return result;
    }


    public List<PropertyCellView> getProperties(AbstractGroup group) {

        List<PropertyCellView> result = new ArrayList<PropertyCellView>();

        for (PropertyCellView property : properties) {
            if (group.hasChild(property.view.view.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<CellView> getCells(AbstractGroup group) {

        List<CellView> result = new ArrayList<CellView>(getProperties(group));

        for (GroupObjectImplementView groupObject : groupObjects)
            for (ObjectImplementView object : groupObject)
                if (group.hasChild(object.view.baseClass.getParent())) {
                    result.add(object.objectCellView);
                    result.add(object.classCellView);
                }

        return result;
    }

    public List<PropertyCellView> getProperties(AbstractGroup group, GroupObjectNavigator groupObject) {

        List<PropertyCellView> result = new ArrayList<PropertyCellView>();

        for (PropertyCellView property : properties) {
            if (groupObject.equals(property.view.toDraw) && group.hasChild(property.view.view.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyCellView> getProperties(Property prop, GroupObjectNavigator groupObject) {

        List<PropertyCellView> result = new ArrayList<PropertyCellView>();

        for (PropertyCellView property : properties) {
            if (groupObject.equals(property.view.toDraw) && prop.equals(property.view.view.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyCellView> getProperties(Property prop) {

        List<PropertyCellView> result = new ArrayList<PropertyCellView>();

        for (PropertyCellView property : properties) {
            if (prop.equals(property.view.view.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyCellView> getProperties(GroupObjectNavigator groupObject) {

        List<PropertyCellView> result = new ArrayList<PropertyCellView>();

        for (PropertyCellView property : properties) {
            if (groupObject.equals(property.view.toDraw)) {
                result.add(property);
            }
        }

        return result;
    }

    public void setFont(Font font, boolean cells) {

        for (CellView property : cells ? getCells() : getProperties()) {
            setFont(property, font);
        }
    }

    public void setFont(AbstractGroup group, Font font) {
        setFont(group, font, false);
    }

    public void setFont(AbstractGroup group, Font font, boolean cells) {

        for (CellView property : cells ? getCells(group) : getProperties(group)) {
            setFont(property, font);
        }
    }

    public void setFont(AbstractGroup group, Font font, GroupObjectNavigator groupObject) {
        
        for (PropertyCellView property : getProperties(group, groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(Font font, GroupObjectNavigator groupObject) {

        for (PropertyCellView property : getProperties(groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(LP property, Font font, GroupObjectNavigator groupObject) {
        setFont(property.property, font, groupObject);
    }

    public void setFont(Property property, Font font, GroupObjectNavigator groupObject) {

        for (PropertyCellView propertyView : getProperties(property, groupObject)) {
            setFont(propertyView, font);
        }
    }

    public void setFont(LP property, Font font) {
        setFont(property.property, font);
    }

    public void setFont(Property property, Font font) {

        for (PropertyCellView propertyView : getProperties(property)) {
            setFont(propertyView, font);
        }
    }

    public void setFont(CellView property, Font font) {
        property.design.font = font;
    }

    public void setBackground(AbstractGroup group, Color background, GroupObjectNavigator groupObject) {

        for (PropertyCellView property : getProperties(group, groupObject)) {
            setBackground(property, background);
        }
    }

    public void setBackground(LP prop, Color background) {
        setBackground(prop.property, background);
    }

    public void setBackground(Property prop, Color background) {

        for (PropertyCellView property : getProperties(prop)) {
            setBackground(property, background);
        }
    }

    public void setBackground(PropertyCellView property, Color background) {
        property.design.background = background;
    }

    public void setFocusable(AbstractGroup group, boolean focusable, GroupObjectNavigator groupObject) {

        for (PropertyCellView property : getProperties(group, groupObject)) {
            setFocusable(property, focusable);
        }
    }

    public void setFocusable(LP property, boolean focusable) {
        setFocusable(property.property, focusable);
    }

    public void setFocusable(LP property, boolean focusable, GroupObjectNavigator groupObject) {
        setFocusable(property.property, focusable, groupObject);
    }

    public void setFocusable(Property property, boolean focusable) {

        for (PropertyCellView propertyView : getProperties(property)) {
            setFocusable(propertyView, focusable);
        }
    }

    public void setFocusable(Property property, boolean focusable, GroupObjectNavigator groupObject) {

        for (PropertyCellView propertyView : getProperties(property, groupObject)) {
            setFocusable(propertyView, focusable);
        }
    }

    public void setFocusable(boolean focusable, GroupObjectNavigator groupObject) {

        for (PropertyCellView propertyView : getProperties(groupObject)) {
            setFocusable(propertyView, focusable);
        }
    }

    public void setFocusable(CellView property, boolean focusable) {
        property.focusable = focusable;
    }

    public void setEditKey(LP property, KeyStroke keyStroke, GroupObjectNavigator groupObject) {
        setEditKey(property.property, keyStroke, groupObject);
    }

    public void setEditKey(LP property, KeyStroke keyStroke) {
        setEditKey(property.property, keyStroke);
    }

    public void setEditKey(Property property, KeyStroke keyStroke, GroupObjectNavigator groupObject) {

        for (PropertyCellView propertyView : getProperties(property, groupObject)) {
            setEditKey(propertyView, keyStroke);
        }
    }

    public void setEditKey(Property property, KeyStroke keyStroke) {

        for (PropertyCellView propertyView : getProperties(property)) {
            setEditKey(propertyView, keyStroke);
        }
    }

    public void setEditKey(CellView property, KeyStroke keyStroke) {
        property.editKey = keyStroke;
    }

    public void setPanelLabelAbove(AbstractGroup group, boolean panelLabelAbove, GroupObjectNavigator groupObject) {

        for (PropertyCellView property : getProperties(group, groupObject)) {
            setPanelLabelAbove(property, panelLabelAbove);
        }
    }

    public void setPanelLabelAbove(AbstractGroup group, boolean panelLabelAbove) {

        for (PropertyCellView property : getProperties(group)) {
            setPanelLabelAbove(property, panelLabelAbove);
        }
    }

    public void setPanelLabelAbove(CellView property, boolean panelLabelAbove) {
        property.panelLabelAbove = panelLabelAbove;
    }
}
