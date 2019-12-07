package lsfusion.gwt.client.form.object.table.grid.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.view.GFormLayoutImpl;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.view.GUserPreferencesDialog;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GCalculateSumButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GCountQuantityButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.grid.view.GGridTable;
import lsfusion.gwt.client.form.object.table.grid.view.GPivot;
import lsfusion.gwt.client.form.object.table.grid.view.GTableView;
import lsfusion.gwt.client.form.property.*;

import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;

public class GGridController extends GAbstractTableController {
    private final ClientMessages messages = ClientMessages.Instance.get();

    private static final GFormLayoutImpl layoutImpl = GFormLayoutImpl.get();

    public GGroupObject groupObject;

    private Panel gridView;
    private ResizableSimplePanel panelView;

    private GTableView table;

    private static boolean isGrid(GGroupObject groupObject) {
        return groupObject != null && groupObject.classView.isGrid();
    }
    public boolean isGrid() {
        return groupObject != null && groupObject.classView.isGrid();
    }

    public GGridController(GFormController iformController) {
        this(iformController, null, null);
    }

    public GGridController(GFormController iformController, GGroupObject igroupObject, GGridUserPreferences[] userPreferences) {
        super(iformController, igroupObject == null ? null : igroupObject.toolbar, isGrid(igroupObject));
        groupObject = igroupObject;

        if (isGrid()) {
            boolean autoSize = groupObject.grid.autoSize;
            
            ResizableSimplePanel panel = new ResizableSimplePanel();
            panel.setStyleName("gridResizePanel");
            if(autoSize) { // убираем default'ый minHeight
                panel.getElement().getStyle().setProperty("minHeight", "0px");
                panel.getElement().getStyle().setProperty("minWidth", "0px");
            }

            this.panelView = panel;
            gridView = layoutImpl.createGridView(panel);

            this.userPreferences = userPreferences;

            getFormLayout().add(groupObject.grid, gridView, () -> {
                Scheduler.get().scheduleDeferred(() -> {
                    table.focus();
                    scrollToTop();
                });
                return true;
            });

            configureToolbar();

            setUpdateMode(false);
            setGridTableView();
        }
    }
    
    private GGridUserPreferences[] userPreferences;
    private void setGridTableView() {
        changeTableView(new GGridTable(formController, this, userPreferences, groupObject.grid.autoSize));
        gridTableButton.showBackground(true);
        pivotTableButton.showBackground(false);
    }
    private void setPivotTableView() {
        changeTableView(new GPivot(formController, this));
        pivotTableButton.showBackground(true);
        gridTableButton.showBackground(false);
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

    private void changeTableView(GTableView table) {
        assert isGrid();

        Widget widget = table.getThisWidget();
        panelView.setWidget(widget);
        setupFillParent(panelView.getElement(), widget.getElement());
        
        this.table = table;
        updateSettingsButton();
    }

    private GCountQuantityButton quantityButton;
    private GCalculateSumButton sumButton;
    private GToolbarButton gridTableButton;
    private GToolbarButton pivotTableButton;
    private GToolbarButton settingsButton;
    private GToolbarButton manualUpdateTableButton;
    private ImageButton forceUpdateTableButton;

    private void configureToolbar() {
        assert isGrid();
        
        addFilterButton();

        if (groupObject.toolbar.showGroupChange) {
            addToolbarSeparator();
            
            addToToolbar(new GToolbarButton("groupchange.png", messages.formGridGroupGroupChange() + " (F12)") {
                @Override
                public void addListener() {
                    addClickHandler(event -> table.groupChange());
                }
            });
        }

        if (groupObject.toolbar.showCountQuantity || groupObject.toolbar.showCalculateSum) {
            addToolbarSeparator();
        }

        if (groupObject.toolbar.showCountQuantity) {
            quantityButton = new GCountQuantityButton() {
                public void addListener() {
                    addClickHandler(event -> formController.countRecords(groupObject));
                }
            };
            addToToolbar(quantityButton);
        }

        if (groupObject.toolbar.showCalculateSum) {
            sumButton = new GCalculateSumButton() {
                @Override
                public void addListener() {
                    addClickHandler(event -> {
                        GPropertyDraw property = getSelectedProperty();
                        if(property != null) {
                            if (property.baseType instanceof GIntegralType) {
                                formController.calculateSum(groupObject, property, table.getCurrentColumn());
                            } else {
                                showSum(null, property);
                            }
                        }
                    });
                }
            };
            addToToolbar(sumButton);
        }

        if (groupObject.toolbar.showPrintGroup || groupObject.toolbar.showPrintGroupXls) {
            addToolbarSeparator();
        }

        if (groupObject.toolbar.showPrintGroup) {
            addToToolbar(new GToolbarButton("reportbw.png", messages.formGridPrintGrid()) {
                @Override
                public void addListener() {
                    addClickHandler(event -> formController.runGroupReport(groupObject.ID, false));
                }
            });
        }

        if (groupObject.toolbar.showPrintGroupXls) {
            addToToolbar(new GToolbarButton("excelbw.png", messages.formGridExportToXlsx()) {
                public void addListener() {
                    addClickHandler(event -> formController.runGroupReport(groupObject.ID, true));
                }
            });
        }

        addToolbarSeparator();

        gridTableButton = new GToolbarButton("grid.png", messages.formGridTableView()) {
            public void addListener() {
                addClickHandler(event -> {
                    setGridTableView();
//                    setUpdateMode(false); //GUpdateMode.AUTO
                    formController.changeMode(groupObject, true, null, null, 0, null, -1, true, null);
                });
            }
        };
        addToToolbar(gridTableButton);

        pivotTableButton = new GToolbarButton("pivot.png", messages.formGridPivotView()) {
            public void addListener() {
                addClickHandler(event -> {
                    setPivotTableView();
//                    setUpdateMode(true); // GUpdateMode.MANUAL
                    formController.changeMode(groupObject, true, new ArrayList<>(), new ArrayList<>(), 0, null, 1000, true, null);
                });
            }
        };
        addToToolbar(pivotTableButton);

        if (groupObject.toolbar.showGridSettings) {
            addToolbarSeparator();

            settingsButton = new GToolbarButton("userPreferences.png", messages.formGridPreferences()) {
                @Override
                public void addListener() {
                    addClickHandler(event -> {
                        changeSettings();
                    });
                }
            };
            addToToolbar(settingsButton);
        }

        addToolbarSeparator();

        manualUpdateTableButton = new GToolbarButton("update.png", messages.formGridManualUpdate()) {
            public void addListener() {
                addClickHandler(event -> {
                    setUpdateMode(!manual);
                    formController.changeMode(groupObject, false, null, null, 0, null, null, false, manual ? GUpdateMode.MANUAL : GUpdateMode.AUTO);
                });
            }
        };
        addToToolbar(manualUpdateTableButton);

        forceUpdateTableButton = new ImageButton(messages.formGridUpdate(), "ok.png");
        forceUpdateTableButton.addClickHandler(event -> {
                    formController.changeMode(groupObject, false, null, null, 0, null, null, false, GUpdateMode.FORCE);
                });
        forceUpdateTableButton.addStyleName("actionPanelRenderer");

        addToToolbar(forceUpdateTableButton);
    }

    public void showRecordQuantity(int quantity) {
        assert isGrid();
        quantityButton.showPopup(quantity);
    }

    public void showSum(Number sum, GPropertyDraw property) {
        assert isGrid();
        sumButton.showPopup(sum, property);
    }

    public void processFormChanges(GFormChanges fc, HashMap<GGroupObject, List<GGroupObjectValue>> currentGridObjects) {
        for (GPropertyDraw property : fc.dropProperties) {
            if (property.groupObject == groupObject) {
                removeProperty(property);
            }
        }

        if (isGrid()) {
            ArrayList<GGroupObjectValue> keys = fc.gridObjects.get(groupObject);
            GTableView table = this.table;
            
            if (keys != null) {
                table.setKeys(keys);
            }

            GGroupObjectValue currentKey = fc.objects.get(groupObject);
            if (currentKey != null) {
                table.setCurrentKey(currentKey);
            }
        }

        // first proceed property with its values, then extra values
        for (Map.Entry<GPropertyReader, HashMap<GGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
            if (readProperty.getKey() instanceof GPropertyDraw) {
                GPropertyDraw property = (GPropertyDraw) readProperty.getKey();
                if (property.groupObject == groupObject) // filling keys
                    updateProperty(property, getColumnKeys(property, currentGridObjects), fc.updateProperties.contains(property), readProperty.getValue());
            }
        }

        for (Map.Entry<GPropertyReader, HashMap<GGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
            if (!(readProperty.getKey() instanceof GPropertyDraw)) {
                GPropertyReader propertyReader = readProperty.getKey();
                if (formController.getGroupObject(propertyReader.getGroupObjectID()) == groupObject) {
                    propertyReader.update(this, readProperty.getValue(), propertyReader instanceof GPropertyDraw && fc.updateProperties.contains(propertyReader));
                }
            }
        }

        Boolean updateState = null;
        if(isGrid())
            updateState = fc.updateStateObjects.get(groupObject);

        update(updateState);
    }

    public List<GGroupObjectValue> getColumnKeys(GPropertyDraw property, HashMap<GGroupObject, List<GGroupObjectValue>> currentGridObjects) {
        List<GGroupObjectValue> columnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        if (property.columnGroupObjects != null) {
            LinkedHashMap<GGroupObject, List<GGroupObjectValue>> groupColumnKeys = new LinkedHashMap<>();
            for (GGroupObject columnGroupObject : property.columnGroupObjects) {
                List<GGroupObjectValue> columnGroupKeys = currentGridObjects.get(columnGroupObject);
                if (columnGroupKeys != null) {
                    groupColumnKeys.put(columnGroupObject, columnGroupKeys);
                }
            }

            columnKeys = GGroupObject.mergeGroupValues(groupColumnKeys);
        }
        return columnKeys;
    }

    @Override
    public void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updateCellBackgroundValues(property, values);
        } else {
            panel.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updateCellForegroundValues(property, values);
        } else {
            panel.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updatePropertyCaptions(property, values);
        } else {
            panel.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updateShowIfValues(property, values);
        } else {
            panel.updateShowIfValues(property, values);
        }
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updateReadOnlyValues(property, values);
        } else {
            panel.updateReadOnlyValues(property, values);
        }
    }

    @Override
    public void updateLastValues(GLastReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        assert property.grid;
        if(property.grid)
            table.updateLastValues(property, reader.index, values);
    }

    @Override
    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        if (isGrid()) {
            table.updateRowBackgroundValues(values);
        } else {
            if (values != null && !values.isEmpty()) {
                panel.updateRowBackgroundValue(values.values().iterator().next());
            }
        }
    }

    @Override
    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        if (isGrid()) {
            table.updateRowForegroundValues(values);
        } else {
            if (values != null && !values.isEmpty()) {
                panel.updateRowForegroundValue(values.values().iterator().next());
            }
        }
    }

    private void removeProperty(GPropertyDraw property) {
        if (property.grid) {
            table.removeProperty(property);
        } else {
            panel.removeProperty(property);
        }
    }
    
    private void updateProperty(GPropertyDraw property, List<GGroupObjectValue> columnKeys, boolean updateKeys, HashMap<GGroupObjectValue, Object> values) {
        if (property.grid) {
            table.updateProperty(property, columnKeys, updateKeys, values);
        } else {
            panel.updateProperty(property, columnKeys, updateKeys, values);
        }
    }

    private void update(Boolean updateState) {
        if (isGrid()) {
            if(updateState != null)
                forceUpdateTableButton.setEnabled(updateState);
            table.update(updateState);

            boolean isVisible = !table.isNoColumns();
            gridView.setVisible(isVisible);

            if (toolbarView != null) {
                toolbarView.setVisible(isVisible);
            }

            if (filter != null) {
                filter.setVisible(isVisible);
            }

            formController.setFiltersVisible(groupObject, isVisible);
        }

        panel.update();
        panel.setVisible(true);
    }

    public void beforeHidingGrid() {
        if (isGrid()) {
            table.beforeHiding();
        }
    }

    public void afterShowingGrid() {
        if (isGrid()) {
            table.afterShowing();
        }
    }

    public void afterAppliedChanges() {
        if (isGrid()) {
            table.afterAppliedChanges();
        }
    }

    public GGroupObjectValue getCurrentKey() {
        GGroupObjectValue result = null;
        if (isGrid()) {
            result = table.getCurrentKey();
        }
        return result == null ? GGroupObjectValue.EMPTY : result;
    }

    @Override
    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        assert this.groupObject.equals(groupObject);
        if(isGrid()) {
            return changeOrders(orders, alreadySet);
        }
        return false; // doesn't matter
    }
    public boolean changeOrders(LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        assert isGrid();
        return table.changePropertyOrders(orders, alreadySet);
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getUserOrders() {
        boolean hasUserPreferences = isGrid() && table.hasUserPreferences();
        if (hasUserPreferences) return table.getUserOrders(getGroupObjectProperties());
        return null;
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getDefaultOrders() {
        return formController.getDefaultOrders(groupObject);
    }

    public GGroupObjectUserPreferences getUserGridPreferences() {
        return table.getCurrentUserGridPreferences();
    }

    public GGroupObjectUserPreferences getGeneralGridPreferences() {
        return table.getGeneralGridPreferences();
    }

    @Override
    public GGroupObject getSelectedGroupObject() {
        return groupObject;
    }

    @Override
    public List<GPropertyDraw> getGroupObjectProperties() {
        ArrayList<GPropertyDraw> properties = new ArrayList<>();
        for (GPropertyDraw property : formController.getPropertyDraws()) {
            if (groupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }
        return properties;
    }
    
    public boolean isPropertyInGrid(GPropertyDraw property) {
        return isGrid() && table.containsProperty(property);
    }

    public int getKeyboardSelectedRow() {
        return table.getKeyboardSelectedRow();
    }

    public boolean isPropertyInPanel(GPropertyDraw property) {
        return panel.containsProperty(property);
    }
    
    @Override
    public GPropertyDraw getSelectedProperty() {
        return table.getCurrentProperty();
    }
    @Override
    public GGroupObjectValue getSelectedColumn() {
        return table.getCurrentColumn();
    }

    @Override
    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return table.getSelectedValue(property, columnKey);
    }

    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
        assert isGrid();

        table.modifyGroupObject(key, add, position);
    }

    public boolean focusFirstWidget() {
        if (isGrid()) {
            GTableView table = this.table;
            if (GwtClientUtils.isShowing(table.getThisWidget())) {
                table.focus();
                return true;
            }
        }

        return panel.focusFirstWidget();
    }

    @Override
    public GComponent getGridComponent() {
        return isGrid() ? groupObject.grid : null;
    }

    @Override
    protected boolean showFilter() {
        return isGrid() && groupObject.filter.visible;
    }

    @Override
    protected void changeFilter(List<GPropertyFilter> conditions) {
        formController.changeFilter(groupObject, conditions);
    }

    public void changeGroupMode(List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeys, int aggrProps, GPropertyGroupType aggrType) {
        formController.changeMode(groupObject, true, properties, columnKeys, aggrProps, aggrType, null, false, null);
    }

    @Override
    public void setFilterVisible(boolean visible) {
        if (isGrid()) {
            super.setFilterVisible(visible);
        }
    }
    
    public void reattachFilter() {
        if (filter != null) {
            filter.reattachDialog();
        }    
    }

    public void focusProperty(GPropertyDraw property) {
        if(property.grid) {
            GTableView table = this.table;
            table.focusProperty(property);
            table.focus();            
        } else {
            panel.focusProperty(property);
        }
    }

    public void changeSettings() {
        if(table instanceof GGridTable) {
            GGridTable gridTable = (GGridTable) table;
            GUserPreferencesDialog dialog = new GUserPreferencesDialog(gridTable, this, formController.hasCanonicalName()) {
                @Override
                public void preferencesChanged() {
                    updateSettingsButton();
                }
            };
            dialog.showDialog();
        } else {
            if(table instanceof GPivot) {
                GPivot pivotTable = (GPivot) table;
                pivotTable.switchSettings();
                updateSettingsButton();
            }
        }
    }

    private void updateSettingsButton() {
        if(table instanceof GGridTable) {
            GGridTable gridTable = (GGridTable) table;
            settingsButton.showBackground(gridTable.hasUserPreferences() || gridTable.generalPreferencesSaved() || gridTable.userPreferencesSaved());
        } else {
            if(table instanceof GPivot) {
                GPivot pivotTable = (GPivot) table;
                settingsButton.showBackground(pivotTable.isSettings());
            }
        }
    }

//    private static void updateTooltip(GGridTable table) {
//        String tooltip = messages.formGridPreferences() + " (";
//        if (table.userPreferencesSaved()) {
//            tooltip += messages.formGridPreferencesSavedForCurrentUser();
//        } else if (table.generalPreferencesSaved()) {
//            tooltip += messages.formGridPreferencesSavedForAllUsers();
//        } else {
//            tooltip += messages.formGridPreferencesNotSaved();
//        }
//
//        setTitle(tooltip + ")");
//    }

}
