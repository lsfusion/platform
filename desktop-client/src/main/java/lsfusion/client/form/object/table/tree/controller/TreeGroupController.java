package lsfusion.client.form.object.table.tree.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.ClientContainerView;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.user.controller.FilterController;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.panel.controller.PanelController;
import lsfusion.client.form.object.table.controller.AbstractTableController;
import lsfusion.client.form.object.table.grid.user.design.view.ExpandTreeButton;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.object.table.tree.TreeGroupNode;
import lsfusion.client.form.object.table.tree.view.TreeGroupTable;
import lsfusion.client.form.object.table.tree.view.TreeView;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.client.form.view.Column;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class TreeGroupController extends AbstractTableController {
    public final ClientTreeGroup treeGroup;
    private final TreeView view;

    private TreeGroupTable tree;

    private final ClientGroupObject lastGroupObject;

    private ExpandTreeButton expandTreeButton = null;
    private ExpandTreeButton expandTreeCurrentButton = null;

    public TreeGroupController(ClientTreeGroup itreeGroup, ClientFormController formController, ClientFormLayout formLayout) throws IOException {
        super(formController, formLayout, itreeGroup.toolbar);
        treeGroup = itreeGroup;

        view = new TreeView(this.formController, treeGroup);
        tree = view.getTree();

        panel = new PanelController(this.formController, formLayout);

        lastGroupObject = BaseUtils.last(treeGroup.groups);

        if (!treeGroup.plainTreeMode) {
            ClientContainerView filtersContainer = formLayout.getContainerView(treeGroup.filtersContainer);
            filter = new FilterController(this, treeGroup.filters, filtersContainer) {
                public void applyFilters(List<ClientPropertyFilter> conditions, boolean focusFirstComponent) {
                    RmiQueue.runAction(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                TreeGroupController.this.formController.changeFilter(treeGroup, conditions);
                                if (focusFirstComponent) {
                                    SwingUtilities.invokeLater(() -> focusFirstComponent());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(ClientResourceBundle.getString("errors.error.applying.filter"), e);
                            }
                        }
                    });
                }
            };

            initFilterButtons();

            filter.addActionsToInputMap(tree);

            addToolbarSeparator();
            
            expandTreeCurrentButton = new ExpandTreeButton(this, true);
            addToToolbar(expandTreeCurrentButton);
            expandTreeButton = new ExpandTreeButton(this, false);
            addToToolbar(expandTreeButton);
        }

        formLayout.addBaseComponent(treeGroup, view);

        // вот так вот приходится делать, чтобы "узнавать" к какому GroupObject относится этот Component
        view.putClientProperty("groupObject", lastGroupObject);
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
                    if (!fc.updateProperties.contains(property) && property.groupObject == group) {
                        addDrawProperty(group, property);

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
                if (propertyRead.getGroupObject() == group) {
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

        tree.updateTable();

        boolean isTreeVisible = tree.getColumnCount() > 1;

        SwingUtils.setGridVisible(view, isTreeVisible);
        
        if (toolbarView != null) {
            SwingUtils.setGridVisible(toolbarView, isTreeVisible);
        }

        filter.update();
        filter.setVisible(isTreeVisible);
        
        for (ClientGroupObject groupObject : treeGroup.groups) {
            formController.setFiltersVisible(groupObject, isTreeVisible);
        }

        panel.update();

        if(expandTreeButton != null) {
            expandTreeButton.update(this);
        }
        if(expandTreeCurrentButton != null) {
            expandTreeCurrentButton.update(this);
        }
    }

    public boolean isCurrentPathExpanded() {
        return tree.isCurrentPathExpanded();
    }

    public ClientGroupObjectValue getCurrentPath() {
        return view.getCurrentPath();
    }

    public void addDrawProperty(ClientGroupObject group, ClientPropertyDraw property) {
        if (property.isList) {
            view.addDrawProperty(group, property);
        } else {
            panel.addProperty(property);
        }
    }

    public void removeProperty(ClientGroupObject group, ClientPropertyDraw property) {
        if(property.isList)
            tree.removeProperty(group, property);
        else
            panel.removeProperty(property);
    }

    @Override
    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            tree.updateCellBackgroundValues(property, values);
        } else {
            panel.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            tree.updateCellForegroundValues(property, values);
        } else {
            panel.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updateImageValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            tree.updateImageValues(property, values);
        } else {
            panel.updateImageValues(property, values);
        }
    }

    @Override
    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update) {
        if (property.isList) {
            view.updateDrawPropertyValues(property, values, update);
        } else {
            panel.updatePropertyValues(property, values, update);
        }
    }

    @Override
    public void updatePropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (!property.isList) {
            panel.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateShowIfValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (!property.isList) {
            panel.updateShowIfValues(property, values);
        }
    }

    @Override
    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            tree.updateReadOnlyValues(property, values);
        } else {
            panel.updateReadOnlyValues(property, values);
        }
    }

    @Override
    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> values) {
        tree.updateRowBackgroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowBackgroundValue((Color) values.values().iterator().next());
    }

    @Override
    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> values) {
        tree.updateRowForegroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowForegroundValue((Color) values.values().iterator().next());
    }

    @Override
    public ClientGroupObject getSelectedGroupObject() {
        TreePath selectedPath = tree.getPathForRow(tree.getSelectedRow());
        if (selectedPath != null) {
            Object node = selectedPath.getLastPathComponent();
            if (node instanceof TreeGroupNode) {
                return ((TreeGroupNode) node).group;
            }
        }
        return treeGroup.groups.get(0);
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
    public List<ClientPropertyDraw> getPropertyDraws() {
        return formController.getPropertyDraws();
    }

    @Override
    public ClientPropertyDraw getSelectedFilterProperty() {
        return tree.getSelectedFilterProperty();
    }

    @Override
    public ClientGroupObjectValue getSelectedColumn() {
        return ClientGroupObjectValue.EMPTY; // пока не поддерживаются группы в колонки
    }

    @Override
    public Object getSelectedValue(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        return tree.getSelectedValue(property);
    }

    @Override
    public List<Pair<Column, String>> getSelectedColumns() {
        return tree.getFilterColumns(getSelectedGroupObject());
    }

    @Override
    public ClientContainer getFiltersContainer() {
        return treeGroup.getFiltersContainer();
    }

    @Override
    public boolean changeOrders(ClientGroupObject groupObject, LinkedHashMap<ClientPropertyDraw, Boolean> value, boolean alreadySet) {
        return tree.changeOrders(groupObject, value, alreadySet);
    }

    public ClientGroupObject getCurrentGroupObject() {
        return ((TreeGroupNode)tree.currentTreePath.getLastPathComponent()).group;
    }

    public boolean focusFirstComponent() {
        return tree.requestFocusInWindow();
    }
}