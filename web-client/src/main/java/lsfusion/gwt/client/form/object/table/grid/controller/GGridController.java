package lsfusion.gwt.client.form.object.table.grid.controller;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.filter.user.GFilter;
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
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButtonGroup;
import lsfusion.gwt.client.form.object.table.grid.view.*;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class GGridController extends GAbstractTableController {
    private final ClientMessages messages = ClientMessages.Instance.get();

    public GGroupObject groupObject;

    private GTableView table;

    public Widget recordView;

    private static boolean isList(GGroupObject groupObject) {
        return groupObject.viewType.isList();
    }
    public boolean isList() {
        return groupObject.viewType.isList();
    }
    
    @Override
    public List<GFilter> getFilters() {
        return groupObject.filters;
    }

    @Override
    public GContainer getFiltersContainer() {
        return groupObject.filtersContainer;
    }

    public GPivotOptions getPivotOptions() {
        return groupObject.pivotOptions;
    }

    public String getMapTileProvider() {
        return groupObject.mapTileProvider;
    }

    public GGridController(GFormController iformController, GGroupObject groupObject, GGridUserPreferences[] userPreferences) {
        super(iformController, groupObject.toolbar, isList(groupObject));
        this.groupObject = groupObject;

        if (isList()) {
            initGridView(groupObject.grid.autoSize);

            // proceeding recordView
            GContainer record = groupObject.grid.record;
            if(record != null) {
                GFormLayout formLayout = getFormLayout();
                GAbstractContainerView recordView = formLayout.getContainerView(record);
                recordView.addUpdateLayoutListener(requestIndex -> table.updateRecordLayout(requestIndex));
                this.recordView = recordView.getView();

                // we need to add recordview somewhere, to attach it (events, listeners, etc.)
                // need to wrap recordView to setVisible false recordView's parent and not recordView itself (since it will be moved and shown by table view implementation)
                formLayout.attachContainer.add(this.recordView);
            }

            this.userPreferences = userPreferences;

            setUpdateMode(false);
            switch (groupObject.listViewType) { // we don't have to do changeListViewType, since it's a first start and it should be set on server
                case PIVOT:
                    setPivotTableView();
                    ((GPivot)table).initDefaultSettings(this);
                    if(!groupObject.asyncInit)
                        ((GPivot)table).setDefaultChangesApplied();
                    break;
                case CUSTOM:
                    setCustomTableView();
                    break;
                case MAP:
                    setMapTableView();
                    break;
                case CALENDAR:
                    setCalendarTableView();
                    break;
                case GRID:
                default:
                    setGridTableView();
            }
            table.setSetRequestIndex(-1);
            updateSettingsButton();
        }
    }

    private GGridUserPreferences[] userPreferences;
    private void setGridTableView() {
        changeTableView(new GGridTable(formController, this, gridView, userPreferences));
        gridTableButton.showBackground(true);
        pivotTableButton.showBackground(false);
        if(mapTableButton != null)
            mapTableButton.showBackground(false);
        if (customViewButton != null)
            customViewButton.showBackground(false);
        if (calendarTableButton != null)
            calendarTableButton.showBackground(false);
    }
    private void setPivotTableView() {
        changeTableView(new GPivot(formController, this, getSelectedProperty()));
        pivotTableButton.showBackground(true);
        gridTableButton.showBackground(false);
        if(mapTableButton != null)
            mapTableButton.showBackground(false);
        if (customViewButton != null)
            customViewButton.showBackground(false);
        if (calendarTableButton != null)
            calendarTableButton.showBackground(false);
    }
    private void setMapTableView() {
        changeTableView(new GMap(formController, this));
        mapTableButton.showBackground(true);
        gridTableButton.showBackground(false);
        pivotTableButton.showBackground(false);
        if (customViewButton != null)
            customViewButton.showBackground(false);
        if (calendarTableButton != null)
            calendarTableButton.showBackground(false);
    }

    private String getCalendarDateType() {

        if (groupObject.isCalendarDate)
            return !groupObject.isCalendarPeriod ? "date" : "dateFrom";

        if (groupObject.isCalendarDateTime)
            return !groupObject.isCalendarPeriod ? "dateTime" : "dateTimeFrom";

        return null;
    }

    private void setCalendarTableView() {
        changeTableView(new GCalendar(formController, this, getCalendarDateType()));
        calendarTableButton.showBackground(true);
        pivotTableButton.showBackground(false);
        gridTableButton.showBackground(false);
        if(mapTableButton != null)
            mapTableButton.showBackground(false);
        if (customViewButton != null)
            customViewButton.showBackground(false);
    }

    private void setCustomTableView() {
        changeTableView(new GCustom(formController, this, groupObject.customRenderFunction));
        if(mapTableButton != null)
            mapTableButton.showBackground(false);
        if (calendarTableButton != null)
            calendarTableButton.showBackground(false);
        gridTableButton.showBackground(false);
        pivotTableButton.showBackground(false);
        customViewButton.showBackground(true);
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
        assert isList();

        if (this.table != null)
            this.table.onClear();

        changeGridView(table, groupObject.grid.isBoxed(table));
        table.onRender(formController.popEditEvent());
        this.table = table;
        updateSettingsButton();
    }

    private GToolbarButton gridTableButton;
    private GToolbarButton pivotTableButton;
    private GToolbarButton customViewButton;
    private GToolbarButton settingsButton;
    private GCountQuantityButton quantityButton;
    private GCalculateSumButton sumButton;
    private GToolbarButton manualUpdateTableButton;
    private GToolbarButton forceUpdateTableButton;

    private GToolbarButton mapTableButton;
    private GToolbarButton calendarTableButton;

    protected void configureToolbar() {
        assert isList();

        GToolbarButtonGroup viewButtonGroup = new GToolbarButtonGroup();
        gridTableButton = new GToolbarButton("grid.png", messages.formGridTableView()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    changeMode(() -> setGridTableView(), GListViewType.GRID, false);
                };
            }
        };
        viewButtonGroup.add(gridTableButton);

        pivotTableButton = new GToolbarButton("pivot.png", messages.formGridPivotView()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    changeMode(() -> setPivotTableView(), GListViewType.PIVOT, true); // we need to make a call to get columns to init default config
                };
            }
        };
        viewButtonGroup.add(pivotTableButton);

        if (groupObject.customRenderFunction != null){
            customViewButton = new GToolbarButton("custom_view.png", messages.formGridCustomView()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> {
                        changeMode(() -> setCustomTableView(), GListViewType.CUSTOM, false);
                    };
                }
            };
            viewButtonGroup.add(customViewButton);
        }

        if(groupObject.isMap) {
            mapTableButton = new GToolbarButton("map.png", messages.formGridMapView()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> {
                        changeMode(() -> setMapTableView(), GListViewType.MAP, false);
                    };
                }
            };
            viewButtonGroup.add(mapTableButton);
        }

        if(getCalendarDateType() != null) {
            calendarTableButton = new GToolbarButton("calendar_view.png", messages.formGridCalendarView()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> {
                        changeMode(() -> setCalendarTableView(), GListViewType.CALENDAR, false);
                    };
                }
            };
            viewButtonGroup.add(calendarTableButton);
        }

        addToToolbar(viewButtonGroup);

        if(showFilter() || groupObject.toolbar.showGridSettings) {

            if (showFilter()) {
                initFilters();
            }

            if (groupObject.toolbar.showGridSettings) {
                GToolbarButtonGroup settingsButtonGroup = new GToolbarButtonGroup();

                settingsButton = new GToolbarButton("userPreferences.png", messages.formGridPreferences()) {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> {
                            changeSettings();
                        };
                    }
                };

                settingsButtonGroup.add(settingsButton);
                addToToolbar(settingsButtonGroup);
            }
        }

        if(groupObject.toolbar.showCountQuantity || groupObject.toolbar.showCalculateSum) {

            GToolbarButtonGroup calculateButtonGroup = new GToolbarButtonGroup();

            if (groupObject.toolbar.showCountQuantity) {
                quantityButton = new GCountQuantityButton() {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> formController.countRecords(groupObject, event.getClientX(), event.getClientY());
                    }
                };
                calculateButtonGroup.add(quantityButton);
            }

            if (groupObject.toolbar.showCalculateSum) {
                sumButton = new GCalculateSumButton() {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> {
                            GPropertyDraw property = getSelectedProperty();
                            if (property != null) {
                                int clientX = event.getClientX();
                                int clientY = event.getClientY();

                                if (property.baseType instanceof GIntegralType)
                                    formController.calculateSum(groupObject, property, table.getCurrentColumnKey(), clientX, clientY);
                                else
                                    showSum(null, property, clientX, clientY);
                            }
                        };
                    }
                };
                calculateButtonGroup.add(sumButton);
            }

            addToToolbar(calculateButtonGroup);
        }

        if(groupObject.toolbar.showPrintGroupXls) {
            GToolbarButtonGroup printButtonGroup = new GToolbarButtonGroup();

            printButtonGroup.add(new GToolbarButton("excelbw.png", messages.formGridExport()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> table.runGroupReport();
                }
            });

            addToToolbar(printButtonGroup);
        }

        GToolbarButtonGroup updateButtonGroup = new GToolbarButtonGroup();

        manualUpdateTableButton = new GToolbarButton("update.png", messages.formGridManualUpdate()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    setUpdateMode(!manual);
                    formController.changeMode(groupObject, false, null, null, 0, null, null, false, manual ? GUpdateMode.MANUAL : GUpdateMode.AUTO, null);
                };
            }
        };
        updateButtonGroup.add(manualUpdateTableButton);

        forceUpdateTableButton = new GToolbarButton(messages.formGridUpdate(), "ok.png", messages.formGridUpdate(), false) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    formController.changeMode(groupObject, false, null, null, 0, null, null, false, GUpdateMode.FORCE, null);
                };
            }
        };
        forceUpdateTableButton.addStyleName("actionPanelRendererValue");
        updateButtonGroup.add(forceUpdateTableButton);

        addToToolbar(updateButtonGroup);
    }

    public void showRecordQuantity(int quantity, int clientX, int clientY) {
        assert isList();
        quantityButton.showPopup(quantity, clientX, clientY);
    }

    public void showSum(Number sum, GPropertyDraw property, int clientX, int clientY) {
        assert isList();
        sumButton.showPopup(sum, property, clientX, clientY);
    }

    public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
        if(isList())
            table.setKeys(keys);
    }

    @Override
    public void updateCurrentKey(GGroupObjectValue currentKey) {
        if(isList())
            table.setCurrentKey(currentKey);
    }

    public void update(long requestIndex, GFormChanges fc) {
        Boolean updateState = null;
        if(isList())
            updateState = fc.updateStateObjects.get(groupObject);

        update(requestIndex, updateState);
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updateCellBackgroundValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updateCellForegroundValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updateImageValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updatePropertyCaptions(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updateLoadings(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updateShowIfValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updatePropertyFooters(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updateReadOnlyValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updateLastValues(formController.getProperty(reader.propertyID), reader.index, values);
    }

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        if (isList()) {
            table.updateRowBackgroundValues(values);
        }
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        if (isList()) {
            table.updateRowForegroundValues(values);
        }
    }

    public GGroupObjectValue getSelectedKey() {
        GGroupObjectValue result = null;
        if (isList()) {
            result = table.getSelectedKey();
        }
        return result == null ? GGroupObjectValue.EMPTY : result;
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

    public GPropertyDraw getSelectedProperty() {
        return table != null ? table.getCurrentProperty() : null;
    }

    @Override
    public GPropertyDraw getSelectedFilterProperty() {
        return getSelectedProperty();
    }

    @Override
    public GGroupObjectValue getSelectedColumnKey() {
        return table.getCurrentColumnKey();
    }

    @Override
    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return table.getSelectedValue(property, columnKey);
    }

    @Override
    public List<Pair<Column, String>> getSelectedColumns() {
        return table.getSelectedColumns();
    }



    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        table.updateProperty(property, columnKeys, updateKeys, values);
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        table.removeProperty(property);
    }

    private void update(long requestIndex, Boolean updateState) {
        if (isList()) {
            if(updateState != null)
                forceUpdateTableButton.setEnabled(updateState);
            table.update(updateState);

            boolean isVisible = !(table.isNoColumns() && requestIndex >= table.getSetRequestIndex());
            GwtClientUtils.setGridVisible(gridView, isVisible);

            if (toolbarView != null)
                GwtClientUtils.setGridVisible(toolbarView, isVisible);

            formController.setFiltersVisible(groupObject, isVisible);
            
            if (filter != null) {
                filter.update();
                filter.setVisible(isVisible);
            }
        }
    }

    @Override
    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        assert this.groupObject.equals(groupObject);
        if(isList()) {
            return changeOrders(orders, alreadySet);
        }
        return false; // doesn't matter
    }
    public boolean changeOrders(LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        assert isList();
        return table.changePropertyOrders(orders, alreadySet);
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getUserOrders() {
        boolean hasUserPreferences = isList() && table.hasUserPreferences();
        if (hasUserPreferences) return table.getUserOrders(getGroupObjectProperties());
        return null;
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getDefaultOrders() {
        return formController.getDefaultOrders(groupObject);
    }

    public List<List<GPropertyDraw>> getPivotColumns() {
        return formController.getPivotColumns(groupObject);
    }

    public List<List<GPropertyDraw>> getPivotRows() {
        return formController.getPivotRows(groupObject);
    }

    public List<GPropertyDraw> getPivotMeasures() {
        return formController.getPivotMeasures(groupObject);
    }

    public GGroupObjectUserPreferences getUserGridPreferences() {
        return table.getCurrentUserGridPreferences();
    }

    public GGroupObjectUserPreferences getGeneralGridPreferences() {
        return table.getGeneralGridPreferences();
    }
    
    public boolean isPropertyInGrid(GPropertyDraw property) {
        return isList() && table.containsProperty(property);
    }

    public int getSelectedRow() {
        return table.getSelectedRow();
    }

    public boolean isPropertyShown(GPropertyDraw property) {
        return table.containsProperty(property);
    }

    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
        assert isList();

        table.modifyGroupObject(key, add, position);
    }

    public boolean focusFirstWidget(FocusUtils.Reason reason) {
        if (table != null && GwtClientUtils.isShowing(table.getWidget())) {
            table.focus(reason);
            return true;
        }

        return false;
    }

    @Override
    public GComponent getGridComponent() {
        return isList() ? groupObject.grid : null;
    }

    @Override
    protected boolean showFilter() {
        return isList();
    }

    @Override
    protected long changeFilter(ArrayList<GPropertyFilter> conditions) {
        return formController.changeFilter(groupObject, conditions);
    }

    private void changeMode(Runnable updateView, GListViewType viewType, boolean setManualUpdateMode) {
        updateView.run();
        table.setSetRequestIndex(formController.changeListViewType(groupObject, table.getPageSize(), viewType, setManualUpdateMode ? GUpdateMode.MANUAL : null));
        updateSettingsButton();
    }
    public void changeGroups(List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeys, int aggrProps, boolean restoreUpdateMode, GPropertyGroupType aggrType) {
        formController.changeMode(groupObject, true, properties, columnKeys, aggrProps, aggrType, null, false, restoreUpdateMode ? (manual ? GUpdateMode.MANUAL : GUpdateMode.AUTO) : null, GListViewType.PIVOT);
    }
    public void changePageSize(int pageSize) {
        formController.changeMode(groupObject, false, null, null, 0, null, pageSize, false, null, null);
    }

    public void focusProperty(GPropertyDraw property) {
        GTableView table = this.table;
        table.focusProperty(property);
    }

    public void changeSettings() {
        if(table instanceof GGridTable) {
            GGridTable gridTable = (GGridTable) table;
            GUserPreferencesDialog dialog = new GUserPreferencesDialog(gridTable, this, formController.panelController, formController.hasCanonicalName()) {
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

    @Override
    public Pair<GGroupObjectValue, Object> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, Object value) {
        if(table instanceof GGridTable) {
            GGridTable gridTable = (GGridTable) table;
            return gridTable.setLoadingValueAt(property, fullCurrentKey, value);
        }
        return null;
    }

    private void updateSettingsButton() {
        if(settingsButton != null) {
            if (table instanceof GGridTable) {
                GGridTable gridTable = (GGridTable) table;
                settingsButton.showBackground(gridTable.hasUserPreferences() || gridTable.generalPreferencesSaved() || gridTable.userPreferencesSaved());
            } else if (table instanceof GPivot) {
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
