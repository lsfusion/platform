package lsfusion.client.form.object.table.grid.controller;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.view.FlatRolloverButton;
import lsfusion.client.base.view.ThemedFlatRolloverButton;
import lsfusion.client.classes.data.ClientIntegralClass;
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
import lsfusion.client.form.object.panel.controller.PropertyPanelController;
import lsfusion.client.form.object.table.controller.AbstractTableController;
import lsfusion.client.form.object.table.grid.user.design.GridUserPreferences;
import lsfusion.client.form.object.table.grid.user.design.view.UserPreferencesDialog;
import lsfusion.client.form.object.table.grid.user.toolbar.view.*;
import lsfusion.client.form.object.table.grid.view.ClientTableView;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.object.table.grid.view.GridView;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.client.form.property.cell.classes.view.link.ImageLinkPropertyRenderer;
import lsfusion.client.form.view.Column;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.client.ClientResourceBundle.getString;

public class GridController extends AbstractTableController {
    private static final String PRINT_XLS_ICON_PATH = "excelbw.png";
    public static final String USER_PREFERENCES_ICON_PATH = "userPreferences.png";
    private static final String UPDATE_ICON_PATH = "update.png";
    private static final String OK_ICON_PATH = "ok.png";

    private final ClientGroupObject groupObject;

    private GridView view;
    public ClientTableView table;

    protected CalculationsView calculationsView;

    private ToolbarGridButton userPreferencesButton;
    private ToolbarGridButton manualUpdateTableButton;
    private FlatRolloverButton forceUpdateTableButton;

    private boolean forceHidden = false;

    public boolean isList() {
        return groupObject != null && groupObject.viewType.isList();
    }

    public GridController(ClientGroupObject igroupObject, ClientFormController formController, final ClientFormLayout formLayout, GridUserPreferences[] userPreferences) {
        super(formController, formLayout, igroupObject == null ? null : igroupObject.toolbar);
        groupObject = igroupObject;

        panel = new PanelController(GridController.this.formController, formLayout) {
            protected void addGroupObjectActions(final JComponent comp) {
                GridController.this.registerGroupObject(comp);
                if (filter != null) {
                    filter.addActionsToPanelInputMap(comp);
                }
            }
        };

        if (groupObject != null) {
            calculationsView = new CalculationsView();
            formLayout.addBaseComponent(groupObject.calculations, calculationsView);

            ClientContainerView filtersContainer = formLayout.getContainerView(groupObject.filtersContainer);
            filter = new FilterController(this, groupObject.filters, filtersContainer) {
                public void applyFilters(List<ClientPropertyFilter> conditions, boolean focusFirstComponent) {
                    RmiQueue.runAction(() -> {
                        try {
                            GridController.this.formController.changeFilter(groupObject, conditions);
                            if (focusFirstComponent) {
                                SwingUtilities.invokeLater(() -> focusFirstComponent());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(ClientResourceBundle.getString("errors.error.applying.filter"), e);
                        }
                    });
                }
            };

            view = new GridView(this, formController, userPreferences, groupObject.grid.tabVertical, groupObject.grid.groupObject.needVerticalScroll);
            table = view.getTable();

            registerGroupObject(view);
            if (table instanceof GridTable) {
                filter.addActionsToInputMap((GridTable) table);
            }

            formLayout.addBaseComponent(groupObject.grid, view);

            configureToolbar();
        }
    }

    private void configureToolbar() {
        if (groupObject.toolbar.showGroupReport && table instanceof GridTable) {
            addToToolbar(new GroupingButton((GridTable) table) {
                @Override
                public List<FormGrouping> readGroupings() {
                    return formController.readGroupings(getGroupObject().getSID());
                }

                @Override
                public Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer,
                        List<byte[]>> sumMap, Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) {
                    return formController.groupData(groupMap, sumMap, maxMap, onlyNotNull);
                }

                @Override
                public void savePressed(FormGrouping grouping) {
                    formController.saveGrouping(grouping);
                }
            });
            addToolbarSeparator();
        }

        boolean showSettings = groupObject.toolbar.showSettings && table instanceof GridTable;
        if(showSettings) {
            initFilterButtons();

            addToolbarSeparator();

            ((GridTable) table).getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    for (int i = 0; i < ((GridTable) table).getTableModel().getColumnCount(); ++i) {
                        ((GridTable) table).setUserWidth(((GridTable) table).getTableModel().getColumnProperty(i), ((GridTable) table).getColumnModel().getColumn(i).getWidth());
                    }
                }
            });
            userPreferencesButton = new ToolbarGridButton(USER_PREFERENCES_ICON_PATH, getUserPreferencesButtonTooltip());
            userPreferencesButton.showBackground(table.hasUserPreferences());

            userPreferencesButton.addActionListener(e -> {
                if(table instanceof GridTable) {
                    UserPreferencesDialog dialog = new UserPreferencesDialog(MainFrame.instance, (GridTable) table, this, getFormController().hasCanonicalName()) {
                        @Override
                        public void preferencesChanged() {
                            RmiQueue.runAction(() -> {
                                userPreferencesButton.showBackground((((GridTable) table).generalPreferencesSaved() || ((GridTable) table).userPreferencesSaved()));
                                userPreferencesButton.setToolTipText(getUserPreferencesButtonTooltip());
                            });
                        }
                    };
                    dialog.setVisible(true);
                }
            });

            addToToolbar(userPreferencesButton);

            addToolbarSeparator();
        }

        boolean showCalculateSum = groupObject.toolbar.showCalculateSum && table instanceof GridTable;
        if(groupObject.toolbar.showCountRows || showCalculateSum) {

            if (groupObject.toolbar.showCountRows) {
                addToToolbar(new CountQuantityButton() {
                    public void addListener() {
                        addActionListener(e -> RmiQueue.runAction(() -> {
                            try {
                                showPopupMenu(formController.countRecords(getGroupObject().getID()));
                            } catch (Exception ex) {
                                throw Throwables.propagate(ex);
                            }
                        }));
                    }
                });
            }

            if (showCalculateSum) {
                addToToolbar(new CalculateSumButton() {
                    public void addListener() {
                        addActionListener(e -> RmiQueue.runAction(() -> {
                            try {
                                ClientPropertyDraw property = table.getCurrentProperty();
                                String caption = property.getPropertyCaption();
                                if (property.baseType instanceof ClientIntegralClass) {
                                    ClientGroupObjectValue columnKey = ((GridTable) table).getTableModel().getColumnKey(Math.max(((GridTable) table).getSelectedColumn(), 0));
                                    Object sum = formController.calculateSum(property.getID(), columnKey.serialize());
                                    showPopupMenu(caption, sum);
                                } else {
                                    showPopupMenu(caption, null);
                                }
                            } catch (Exception ex) {
                                throw Throwables.propagate(ex);
                            }
                        }));
                    }
                });
            }
            addToolbarSeparator();
        }

        if(groupObject.toolbar.showXls) {
            addToToolbar(new ToolbarGridButton(PRINT_XLS_ICON_PATH, getString("form.grid.export.to.xlsx")) {
                @Override
                public void addListener() {
                    addActionListener(e -> RmiQueue.runAction(() -> formController.runSingleGroupXlsExport(GridController.this)));
                }
            });

            addToolbarSeparator();
        }

        manualUpdateTableButton = new ToolbarGridButton(UPDATE_ICON_PATH, getString("form.grid.manual.update")) {
            @Override
            public void addListener() {
                addActionListener(e -> RmiQueue.runAction(() -> {
                    setUpdateMode(!manual);
                    formController.changeMode(groupObject, manual ? UpdateMode.MANUAL : UpdateMode.AUTO);
                }));
            }
        };
        addToToolbar(manualUpdateTableButton);

        forceUpdateTableButton = new ThemedFlatRolloverButton(OK_ICON_PATH, getString("form.grid.update"));
        forceUpdateTableButton.setAlignmentY(Component.TOP_ALIGNMENT);
        forceUpdateTableButton.addActionListener(e -> RmiQueue.runAction(() -> {
            formController.changeMode(groupObject, UpdateMode.FORCE);
        }));
        forceUpdateTableButton.setFocusable(false);
        forceUpdateTableButton.setVisible(false);
        addToToolbar(forceUpdateTableButton);
    }

    private boolean manual;
    private void setUpdateMode(boolean manual) {
        this.manual = manual;
        if(manual) {
            forceUpdateTableButton.setVisible(true);
            forceUpdateTableButton.setEnabled(false);
        } else
            forceUpdateTableButton.setVisible(false);
        manualUpdateTableButton.showBackground(manual);
    }

    private String getUserPreferencesButtonTooltip() {
        String tooltip = getString("form.grid.preferences") + " (";
        if (((GridTable) table).userPreferencesSaved()) {
            tooltip += getString("form.grid.preferences.saved.for.current.users");
        } else if (((GridTable) table).generalPreferencesSaved()) {
            tooltip += getString("form.grid.preferences.saved.for.all.users");
        } else {
            tooltip += getString("form.grid.preferences.not.saved");
        }
        return tooltip + ")";
    }

    public void processFormChanges(ClientFormChanges fc,
                                   Map<ClientGroupObject, List<ClientGroupObjectValue>> cachedGridObjects
    ) {

        // Сначала меняем виды объектов
        for (ClientPropertyReader read : fc.properties.keySet()) // интересуют только свойства
        {
            if (read instanceof ClientPropertyDraw) {
                ClientPropertyDraw property = (ClientPropertyDraw) read;
                if (property.groupObject == groupObject && !fc.updateProperties.contains(property)) {
                    ImageLinkPropertyRenderer.clearChache(property);

                    addDrawProperty(property);

                    OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>> groupColumnKeys = new OrderedMap<>();
                    for (ClientGroupObject columnGroupObject : property.columnGroupObjects) {
                        if (cachedGridObjects.containsKey(columnGroupObject)) {
                            groupColumnKeys.put(columnGroupObject, cachedGridObjects.get(columnGroupObject));
                        }
                    }

                    updateDrawColumnKeys(property, ClientGroupObject.mergeGroupValues(groupColumnKeys));
                }
            }
        }

        for (ClientPropertyDraw property : fc.dropProperties) {
            if (property.groupObject == groupObject) {
                removeProperty(property);
            }
        }

        if (isList()) {
            if (fc.gridObjects.containsKey(groupObject)) {
                setRowKeysAndCurrentObject(fc.gridObjects.get(groupObject), fc.objects.get(groupObject));
            }
        }

        // Затем их свойства
        for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
            ClientPropertyReader propertyRead = readProperty.getKey();
            if (propertyRead.getGroupObject() == groupObject) {
                propertyRead.update(readProperty.getValue(), fc.updateProperties.contains(propertyRead), this);
            }
        }

        Boolean updateState = null;
        if(isList())
            updateState = fc.updateStateObjects.get(groupObject);

        update(updateState);
    }

    public void addDrawProperty(ClientPropertyDraw property) {
        if (property.isList) {
            table.addProperty(property);
        } else {
            panel.addProperty(property);
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
        if (property.isList) {
            table.removeProperty(property);
        } else {
            panel.removeProperty(property);
        }
    }

    public void setRowKeysAndCurrentObject(List<ClientGroupObjectValue> gridObjects, ClientGroupObjectValue newCurrentObject) {
        table.setRowKeysAndCurrentObject(gridObjects, newCurrentObject);
    }

    public void modifyGroupObject(ClientGroupObjectValue gridObject, boolean add, int position) {
        assert isList();

        table.modifyGroupObject(gridObject, add, position); // assert что grid!=null

        updateTable(null);
    }

    public ClientGroupObjectValue getCurrentKey() {
        ClientGroupObjectValue result = null;
        if (isList()) {
            result = table.getCurrentKey();
        }
        return result == null ? ClientGroupObjectValue.EMPTY : result;
    }
    
    public int getCurrentRow() {
        return table != null ? table.getCurrentRow() : -1;
    }

    public void updateDrawColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> groupColumnKeys) {
        if (property.isList) {
            table.updateColumnKeys(property, groupColumnKeys);
        } else {
            panel.updateColumnKeys(property, groupColumnKeys);
        }
    }

    @Override
    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            table.updateCellBackgroundValues(property, values);
        } else {
            panel.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            table.updateCellForegroundValues(property, values);
        } else {
            panel.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updateImageValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            table.updateImageValues(property, values);
        } else {
            panel.updateImageValues(property, values);
        }
    }

    @Override
    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean updateKeys) {
        if (property.isList) {
            table.updatePropertyValues(property, values, updateKeys);
        } else {
            panel.updatePropertyValues(property, values, updateKeys);
        }
    }

    @Override
    public void updatePropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            table.updatePropertyCaptions(property, values);
        } else {
            panel.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateShowIfValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            table.updateShowIfValues(property, values);
        } else {
            panel.updateShowIfValues(property, values);
        }
    }

    @Override
    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (property.isList) {
            table.updateReadOnlyValues(property, values);
        } else {
            panel.updateReadOnlyValues(property, values);
        }
    }

    @Override
    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> values) {
        if (isList()) {
            table.updateRowBackgroundValues(values);
        } else {
            if (values != null && !values.isEmpty()) {
                panel.updateRowBackgroundValue((Color) values.values().iterator().next());
            }
        }
    }

    @Override
    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> values) {
        if (isList()) {
            table.updateRowForegroundValues(values);
        } else {
            if (values != null && !values.isEmpty()) {
                panel.updateRowForegroundValue((Color) values.values().iterator().next());
            }
        }
    }

    @Override
    public ClientGroupObject getSelectedGroupObject() {
        return getGroupObject();
    }

    @Override
    public ClientGroupObject getGroupObject() {
        return groupObject;
    }

    @Override
    public List<ClientPropertyDraw> getGroupObjectProperties() {
        ArrayList<ClientPropertyDraw> properties = new ArrayList<>();
        for (ClientPropertyDraw property : getPropertyDraws()) {
            if (groupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }
        return properties;
    }

    @Override
    public List<ClientPropertyDraw> getPropertyDraws() {
        return formController.form.getPropertyDraws();
    }

    @Override
    public ClientPropertyDraw getSelectedFilterProperty() {
        return table.getCurrentProperty();
    }

    @Override
    public ClientGroupObjectValue getSelectedColumn() {
        return table.getCurrentColumn();
    }

    @Override
    public Object getSelectedValue(ClientPropertyDraw cell, ClientGroupObjectValue columnKey) {
        return table.getSelectedValue(cell, columnKey);
    }

    @Override
    public List<Pair<Column, String>> getSelectedColumns() {
        return table.getFilterColumns();
    }

    @Override
    public ClientContainer getFiltersContainer() {
        return getGroupObject().getFiltersContainer();
    }

    @Override
    public boolean changeOrders(ClientGroupObject groupObject, LinkedHashMap<ClientPropertyDraw, Boolean> orders, boolean alreadySet) {
        assert this.groupObject.equals(groupObject);
        if(isList()) {
            return changeOrders(orders, alreadySet);
        }
        return false; // doesn't matter
    }

    public boolean changeOrders(LinkedHashMap<ClientPropertyDraw, Boolean> orders, boolean alreadySet) {
        assert isList();
        return table.changePropertyOrders(orders, alreadySet);
    }

    public OrderedMap<ClientPropertyDraw, Boolean> getUserOrders() {
        boolean hasUserPreferences = isList() && table.hasUserPreferences();
        if (hasUserPreferences) return table.getUserOrders(getGroupObjectProperties());
        return null;
    }

    public OrderedMap<ClientPropertyDraw, Boolean> getDefaultOrders() {
        return formController.getDefaultOrders(groupObject);
    }
    
    public GroupObjectUserPreferences getUserGridPreferences() {
        return table.getCurrentUserGridPreferences();
    }

    public GroupObjectUserPreferences getGeneralGridPreferences() {
        return table.getGeneralGridPreferences();
    }

    public void registerGroupObject(JComponent comp) {
        comp.putClientProperty("groupObject", groupObject);
    }
    
    public boolean isPropertyInGrid(ClientPropertyDraw property) {
        return table != null && table.containsProperty(property);
    }
    
    public boolean isPropertyInPanel(ClientPropertyDraw property) {
        return panel.containsProperty(property);
    }

    public boolean isPropertyShown(ClientPropertyDraw property) {
        if(property.isList)
            return table.containsProperty(property);
        else
            return panel.containsProperty(property);
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        if (filter.hasFiltersContainer()) {
            filter.quickEditFilter(initFilterKeyEvent, propertyDraw, columnKey);
        }
    }

    public void selectProperty(ClientPropertyDraw propertyDraw) {
        table.focusProperty(propertyDraw);
    }

    public boolean focusProperty(ClientPropertyDraw propertyDraw) {
        PropertyPanelController propertyPanelController = panel.getPropertyController(propertyDraw);
        if (propertyPanelController != null) {
            return propertyPanelController.requestFocusInWindow();
        } else {
            table.focusProperty(propertyDraw);
            return table.requestFocusInWindow();
        }
    }

    public void updateSelectionInfo(int quantity, String sum, String avg) {
        if (calculationsView != null) {
            calculationsView.updateSelectionInfo(quantity, sum, avg);
        }
    }

    private void update(Boolean updateState) {
        if (groupObject != null) {
            if(updateState != null)
                forceUpdateTableButton.setEnabled(updateState);
            updateTable(updateState);

            if (toolbarView != null) {
                toolbarView.setVisible(isVisible());
            }

            filter.update();
            filter.setVisible(isVisible());

            if (calculationsView != null) {
                calculationsView.setVisible(isVisible());
            }

            formController.setFiltersVisible(groupObject, isVisible());
        }

        panel.update();
        panel.setVisible(true);
    }

    public GridView getGridView() {
        return view;
    }

    public boolean getAutoSize() {
        return groupObject.grid.autoSize;
    }

    public void setForceHidden(boolean forceHidden) {
        this.forceHidden = forceHidden;
    }

    public boolean isVisible() {
        return !forceHidden && isList();
    }

    public void updateTable(Boolean updateState) {
        table.update(updateState);
        view.setVisible(isVisible());
    }

    public boolean focusFirstComponent() {
        if (table != null) {
            return table.requestFocusInWindow();
        }
        return false;
    }
}