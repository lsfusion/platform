package platform.client.form.tree;

import platform.base.BaseUtils;
import platform.client.ClientResourceBundle;
import platform.client.form.AbstractGroupObjectController;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.LogicsSupplier;
import platform.client.form.panel.PanelController;
import platform.client.form.queries.FilterController;
import platform.client.logics.*;
import platform.interop.Order;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TreeGroupController extends AbstractGroupObjectController {
    public final ClientTreeGroup treeGroup;
    private final TreeView view;

    private TreeGroupTable tree;

    private final ClientGroupObject lastGroupObject;

    public TreeGroupController(ClientTreeGroup itreeGroup, LogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout formLayout) throws IOException {
        super(iform, ilogicsSupplier, formLayout, itreeGroup.toolbar);
        treeGroup = itreeGroup;

        view = new TreeView(this.form, treeGroup);
        tree = view.getTree();

        panel = new PanelController(this, form, formLayout);

        lastGroupObject = BaseUtils.last(treeGroup.groups);

        if (!treeGroup.plainTreeMode) {
            FilterController filter = new FilterController(this, treeGroup.filter) {
                protected void remoteApplyQuery() {
                    try {
                        form.changeFilter(treeGroup, getConditions());
                    } catch (IOException e) {
                        throw new RuntimeException(ClientResourceBundle.getString("errors.error.applying.filter"), e);
                    }

                    tree.requestFocusInWindow();
                }
            };

            filter.addView(formLayout);

            addToToolbar(filter.getToolbarButton());

            filter.getView().addActionsToInputMap(tree);
        }

        formLayout.add(treeGroup, view);
    }

    public void processFormChanges(ClientFormChanges fc,
                                   Map<ClientGroupObject, List<ClientGroupObjectValue>> cachedGridObjects) {

        tree.saveVisualState();
        for (ClientGroupObject group : treeGroup.groups) {
            if (fc.gridObjects.containsKey(group)) {
                view.updateKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group));
            }

            // добавляем новые свойства
            for (ClientPropertyReader read : fc.properties.keySet()) {
                if (read instanceof ClientPropertyDraw) {
                    ClientPropertyDraw property = (ClientPropertyDraw) read;
                    if (!fc.updateProperties.contains(property) && property.groupObject == group && property.shouldBeDrawn(form)) {
                        addDrawProperty(group, property, fc.panelProperties.contains(property));

                        //пока не поддерживаем группы в колонках в дереве, поэтому делаем
                        if (panelProperties.contains(property)) {
                            panel.updateColumnKeys(property, Collections.singletonList(ClientGroupObjectValue.EMPTY));
                        }
                    }
                }
            }

            // удаляем ненужные
            for (ClientPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == group) {
                    removeProperty(group, property);
                }
            }

            // обновляем значения свойств
            for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
                ClientPropertyReader propertyRead = readProperty.getKey();
                if (propertyRead.getGroupObject() == group && propertyRead.shouldBeDrawn(form)) {
                    propertyRead.update(readProperty.getValue(), fc.updateProperties.contains(propertyRead), this);
                }
            }

            if (fc.objects.containsKey(group)) {
                view.setCurrentPath(fc.objects.get(group));
            }
        }

        panel.update();
        tree.restoreVisualState();
    }

    public ClientGroupObjectValue getCurrentPath() {
        return view.getCurrentPath();
    }

    public void addDrawProperty(ClientGroupObject group, ClientPropertyDraw property, boolean toPanel) {
        if (toPanel) {
            panelProperties.add(property);
            panel.addProperty(property);
            view.removeProperty(group, property);
        } else {
            panelProperties.remove(property);
            view.addDrawProperty(group, property, toPanel);
            panel.removeProperty(property);
        }
    }

    public void removeProperty(ClientGroupObject group, ClientPropertyDraw property) {
        panelProperties.remove(property);
        view.removeProperty(group, property);
        panel.removeProperty(property);
    }

    @Override
    public ClientGroupObject getGroupObject() {
        return lastGroupObject;
    }

    @Override
    public List<ClientPropertyDraw> getGroupObjectProperties() {
        ClientGroupObject currentGroupObject = getSelectedGroupObject();

        ArrayList<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();
        for (ClientPropertyDraw property : getPropertyDraws()) {
            if (currentGroupObject != null && currentGroupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }

        return properties;
    }

    @Override
    public ClientPropertyDraw getSelectedProperty() {
        ClientPropertyDraw defaultProperty = lastGroupObject.filterProperty;
        return defaultProperty != null
                ? defaultProperty
                : tree.getCurrentProperty();
    }

    @Override
    public Object getSelectedValue(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        return tree.getSelectedValue(property);
    }

    public void changeOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        tree.changeOrder(property, modiType);
    }

    @Override
    public void clearOrders() throws IOException {
        tree.clearOrders(getGroupObject());
    }

    @Override
    public List<ClientPropertyDraw> getPropertyDraws() {
        return form.getPropertyDraws();
    }

    @Override
    public ClientGroupObject getSelectedGroupObject() {
        Object node = tree.currentTreePath.getLastPathComponent();
        return node instanceof TreeGroupNode
               ? ((TreeGroupNode) node).group
               : treeGroup.groups.get(0);
    }

    public void updateDrawPropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        if (panelProperties.contains(property)) {
            panel.updatePropertyCaptions(property, captions);
        }
    }

    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> rowBackground) {
        panel.updateRowBackgroundValue((Color)BaseUtils.singleValue(rowBackground));
    }

    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground) {
        panel.updateRowForegroundValue((Color)BaseUtils.singleValue(rowForeground));
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update) {
        if (panelProperties.contains(property)) {
            panel.updatePropertyValues(property, values, update);
        } else
            view.updateDrawPropertyValues(property, values, update);
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        if (panelProperties.contains(property)) {
            panel.updateCellBackgroundValue(property, cellBackgroundValues);
        }
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        if (panelProperties.contains(property)) {
            panel.updateCellForegroundValue(property, cellForegroundValues);
        }
    }
}