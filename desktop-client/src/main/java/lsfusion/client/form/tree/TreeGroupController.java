package lsfusion.client.form.tree;

import lsfusion.base.BaseUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.AbstractGroupObjectController;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.LogicsSupplier;
import lsfusion.client.form.RmiQueue;
import lsfusion.client.form.layout.ClientFormLayout;
import lsfusion.client.form.panel.PanelController;
import lsfusion.client.form.queries.FilterController;
import lsfusion.client.logics.*;
import lsfusion.interop.Order;

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

        panel = new PanelController(form, formLayout);

        lastGroupObject = BaseUtils.last(treeGroup.groups);

        if (!treeGroup.plainTreeMode) {
            filter = new FilterController(this, treeGroup.filter) {
                protected void remoteApplyQuery() {
                    RmiQueue.runAction(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                form.changeFilter(treeGroup, getConditions());
                                tree.requestFocusInWindow();
                            } catch (IOException e) {
                                throw new RuntimeException(ClientResourceBundle.getString("errors.error.applying.filter"), e);
                            }
                        }
                    });
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
                view.updateKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group), fc.expandables.get(group));
            }

            // добавляем новые свойства
            for (ClientPropertyReader read : fc.properties.keySet()) {
                if (read instanceof ClientPropertyDraw) {
                    ClientPropertyDraw property = (ClientPropertyDraw) read;
                    if (!fc.updateProperties.contains(property) && property.groupObject == group && property.shouldBeDrawn(form)) {
                        addDrawProperty(group, property, fc.panelProperties.contains(property));

                        //пока не поддерживаем группы в колонках в дереве, поэтому делаем
                        if (panel.containsProperty(property)) {
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

        update();
    }

    private void update() {
        tree.restoreVisualState();

        boolean isTreeVisible = tree.getColumnCount() > 1;
        
        view.setVisible(isTreeVisible);
        
        if (toolbarView != null) {
            toolbarView.setVisible(isTreeVisible);
        }

        if (filter != null) {
            filter.setVisible(isTreeVisible);
        }
        
        for (ClientGroupObject groupObject : treeGroup.groups) {
            form.setFiltersVisible(groupObject, isTreeVisible);
        }

        panel.update();
    }

    public ClientGroupObjectValue getCurrentPath() {
        return view.getCurrentPath();
    }

    public void addDrawProperty(ClientGroupObject group, ClientPropertyDraw property, boolean toPanel) {
        if (toPanel) {
            panel.addProperty(property);
            view.removeProperty(group, property);
        } else {
            view.addDrawProperty(group, property, toPanel);
            panel.removeProperty(property);
        }
    }

    public void removeProperty(ClientGroupObject group, ClientPropertyDraw property) {
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

        ArrayList<ClientPropertyDraw> properties = new ArrayList<>();
        for (ClientPropertyDraw property : getPropertyDraws()) {
            if (currentGroupObject != null && currentGroupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }

        return properties;
    }

    @Override
    public ClientPropertyDraw getSelectedProperty() {
        return tree.getCurrentProperty();
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
        if (panel.containsProperty(property)) {
            panel.updatePropertyCaptions(property, captions);
        }
    }

    public void updateShowIfs(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> showIfs) {
        if (panel.containsProperty(property)) {
            panel.updateShowIfs(property, showIfs);
        }
    }

    @Override
    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (panel.containsProperty(property)) {
            panel.updateReadOnlyValues(property, values);
        } else {
            tree.updateReadOnlyValues(property, values);
        }
    }

    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> rowBackground) {
        tree.updateRowBackgroundValues(rowBackground);
        panel.updateRowBackgroundValue((Color)BaseUtils.singleValue(rowBackground));
    }

    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground) {
        tree.updateRowForegroundValues(rowForeground);
        panel.updateRowForegroundValue((Color)BaseUtils.singleValue(rowForeground));
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update) {
        if (panel.containsProperty(property)) {
            panel.updatePropertyValues(property, values, update);
        } else
            view.updateDrawPropertyValues(property, values, update);
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        if (panel.containsProperty(property)) {
            panel.updateCellBackgroundValues(property, cellBackgroundValues);
        } else {
            tree.updateCellBackgroundValues(property, cellBackgroundValues);
        }
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        if (panel.containsProperty(property)) {
            panel.updateCellForegroundValues(property, cellForegroundValues);
        } else {
            tree.updateCellForegroundValues(property, cellForegroundValues);
        }
    }
}