package lsfusion.gwt.client.form.object.table.grid.controller;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPlatformTableController;
import lsfusion.gwt.client.form.object.table.controller.GGridToolbarController;
import lsfusion.gwt.client.form.object.table.controller.GListViewController;
import lsfusion.gwt.client.form.object.table.controller.GPlatformGridToolbarController;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.view.GUserPreferencesDialog;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GCalculateSumButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButtonGroup;
import lsfusion.gwt.client.form.object.table.grid.view.*;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.List;

public final class GPlatformGridController extends GPlatformTableController<GGrid, GListView, GGridToolbarController> implements GGridController, GListViewController {
    private final ClientMessages messages = ClientMessages.Instance.get();

    public GGroupObject groupObject;

    public Widget recordView;

    // the export, which a pivot disables while it fills its table - on the platform's toolbar only, where the design
    // shows it (addToolbarButtons)
    public GToolbarButton exportToExcelButton;

    public GPivotOptions getPivotOptions() {
        return groupObject.pivotOptions;
    }

    public String getMapTileProvider() {
        return groupObject.mapTileProvider;
    }

    public GPlatformGridController(GFormController iformController, GGroupObject groupObject, GGridUserPreferences[] userPreferences, GGridToolbarController toolbar) {
        super(iformController, groupObject.grid, toolbar);
        this.groupObject = groupObject;

        initGridView();
        // the table given to its toolbar - the platform's takes its buttons, a view's part its views -, before the
        // view it shows is set, which the toolbar shows; the filter input controller, whose button the toolbar takes
        // too, is made over the table once it is made, and bound to it
        // (GFormController.initializeTable)
        toolbar.addRows(this);

        // proceeding recordView
        GContainer record = table.record;
        if(record != null) {
            GFormLayout formLayout = getFormLayout();
            GAbstractContainerView recordView = formLayout.getContainerView(record);
            recordView.addUpdateLayoutListener(requestIndex -> view.updateRecordLayout(requestIndex));
            this.recordView = recordView.getView();

            // we need to add recordview somewhere, to attach it (events, listeners, etc.)
            // need to wrap recordView to setVisible false recordView's parent and not recordView itself (since it will be moved and shown by table view implementation)
            formLayout.attachContainer.add(this.recordView);
        }

        this.userPreferences = userPreferences;

        // we don't have to do changeListViewType, since it's a first start and it should be set on server
        setListView(groupObject.listViewType);
        if (groupObject.listViewType == GListViewType.PIVOT) {
            ((GPivot)view).initDefaultSettings(this);
            if(!groupObject.asyncInit)
                ((GPivot)view).setDefaultChangesApplied();
        }
        view.setSetRequestIndex(-1);
        updateSettingsButton();
    }

    private GGridUserPreferences[] userPreferences;

    private List<GPropertyDraw> getCalendarDateProps() {
        return groupObject.calendarDateProps != null ? groupObject.calendarDateProps : new ArrayList<>();
    }

    // ===== the views its rows can be shown as (GListViewController): those the design gives them, in the order the
    // toolbar's switch has them, the one shown - which the switch shows -, and another shown instead, as the switch
    // asks
    private GListViewType listView;

    public List<GListViewType> getListViews() {
        List<GListViewType> views = new ArrayList<>();
        views.add(GListViewType.GRID);
        views.add(GListViewType.PIVOT);
        if (groupObject.customRenderFunction != null)
            views.add(GListViewType.CUSTOM);
        if (groupObject.isMap)
            views.add(GListViewType.MAP);
        if (!getCalendarDateProps().isEmpty())
            views.add(GListViewType.CALENDAR);
        return views;
    }

    public GListViewType getListView() {
        return listView;
    }

    public void changeListView(GListViewType type) {
        setListView(type);
        // a pivot makes a call to get the columns, to init its default config
        view.setSetRequestIndex(formController.changeListViewType(groupObject, view.getPageSize(), type, type == GListViewType.PIVOT ? GUpdateMode.MANUAL : null));
        updateSettingsButton();
    }

    // ... and the view of each: the rows shown as one of those views, at first or when the switch asks
    private void setListView(GListViewType type) {
        GListView view;
        switch (type) {
            case PIVOT:
                view = new GPivot(formController, this, getSelectedProperty(), gridView);
                break;
            case CUSTOM:
                view = new GCustom(formController, this, gridView, groupObject.customRenderFunction);
                break;
            case MAP:
                view = new GMap(formController, this, gridView);
                break;
            case CALENDAR:
                view = new GCalendar(formController, this, gridView, getCalendarDateProps());
                break;
            default:
                view = new GGridTable(formController, this, gridView, userPreferences);
        }
        changeTableView(view);
        listView = type;
        toolbar.showView(type);
    }

    private void changeTableView(GListView view) {
        if (this.view != null) {
            this.view.onClear();
            // the replaced view is dropped, and it is registered in the global color-theme listener list
            this.view.destroy();
        }

        changeGridView(view);
        view.onRender(formController.popEditEvent());
        this.view = view;
        updateSettingsButton();
    }

    // the settings, on the platform's toolbar only, where the design shows them (addToolbarButtons)
    private GToolbarButton settingsButton;

    // the table's buttons on the platform's toolbar, in their order (GGridController), its view switch first - the
    // filter controller's button goes after it (GPlatformGridToolbarController.getFilterToggleIndex)
    public void addToolbarButtons(GPlatformGridToolbarController platformToolbar) {
        if (table.toolbar.showViews)
            platformToolbar.addViewSwitch(this);

        if (table.toolbar.showSettings) {
            GToolbarButtonGroup settingsButtonGroup = new GToolbarButtonGroup();

            settingsButton = new GToolbarButton(StaticImage.USERPREFERENCES, messages.formGridPreferences()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> {
                        changeSettings();
                    };
                }
            };

            settingsButtonGroup.add(settingsButton);
            platformToolbar.add(settingsButtonGroup);
        }

        if(table.toolbar.showCountQuantity || table.toolbar.showCalculateSum) {

            GToolbarButtonGroup calculateButtonGroup = new GToolbarButtonGroup();

            if (table.toolbar.showCountQuantity) {
                calculateButtonGroup.add(platformToolbar.createCountButton());
            }

            if (table.toolbar.showCalculateSum) {
                calculateButtonGroup.add(new GCalculateSumButton() {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> {
                            GPropertyDraw property = getSelectedProperty();
                            if (property != null) {
                                if (property.getValueType() instanceof GIntegralType)
                                    formController.calculateSum(groupObject, property, view.getCurrentColumnKey(), sum -> showPopup(sum, property), error -> { });
                                else
                                    showPopup(null, property);
                            }
                        };
                    }
                });
            }

            platformToolbar.add(calculateButtonGroup);
        }

        if(table.toolbar.showPrintGroupXls) {
            exportToExcelButton = platformToolbar.addExport(() -> view.runGroupReport());
        }
    }
    // ... and its views, which a view's part of the toolbar switches: the table's own (GListViewController)
    public GListViewController getListViewController() {
        return this;
    }

    public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
        view.setKeys(keys);
    }

    // hidden with no columns once the answer to the request that set the view has come - before it, it has none yet
    @Override
    protected boolean updateView(long requestIndex, GFormChanges fc) {
        view.update(fc.updateStateObjects.get(groupObject));
        return !(view.isNoColumns() && requestIndex >= view.getSetRequestIndex());
    }

    @Override
    public GGroupObject getSelectedGroupObject() {
        return groupObject;
    }

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
        return view != null ? view.getCurrentProperty() : null;
    }

    @Override
    public GPropertyDraw getSelectedFilterProperty() {
        return getSelectedProperty();
    }

    @Override
    public GGroupObjectValue getSelectedColumnKey() {
        return view.getCurrentColumnKey();
    }

    @Override
    public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return view.getSelectedValue(property, columnKey);
    }

    @Override
    public List<Pair<Column, String>> getFilterColumns() {
        return view.getFilterColumns();
    }

    public ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> getPivotColumns() {
        return formController.getPivotColumns(groupObject);
    }

    public ArrayList<ArrayList<GPropertyDrawOrPivotColumn>> getPivotRows() {
        return formController.getPivotRows(groupObject);
    }

    public ArrayList<GPropertyDraw> getPivotMeasures() {
        return formController.getPivotMeasures(groupObject);
    }

    public GGroupObjectUserPreferences getUserGridPreferences() {
        return view.getCurrentUserGridPreferences();
    }

    public GGroupObjectUserPreferences getGeneralGridPreferences() {
        return view.getGeneralGridPreferences();
    }

    // its views' user preferences, the form's to save and a report's to read (GPlatformTableController)
    @Override
    public void addUserPreferences(List<GGroupObjectUserPreferences> user, List<GGroupObjectUserPreferences> general) {
        user.add(getUserGridPreferences());
        general.add(getGeneralGridPreferences());
    }

    public void changeGroups(List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeys, int aggrProps, boolean restoreUpdateMode, GPropertyGroupType aggrType) {
        formController.changeMode(groupObject, true, properties, columnKeys, aggrProps, aggrType, null, false, restoreUpdateMode ? (formController.isManualUpdate(groupObject) ? GUpdateMode.MANUAL : GUpdateMode.AUTO) : null, GListViewType.PIVOT);
    }
    public void changePageSize(int pageSize) {
        formController.changeMode(groupObject, false, null, null, 0, null, pageSize, false, null, null);
    }

    public void changeSettings() {
        if(view instanceof GGridTable) {
            GGridTable gridTable = (GGridTable) view;
            GUserPreferencesDialog dialog = new GUserPreferencesDialog(gridTable, this, formController.platformFormPanelController, formController.hasCanonicalName()) {
                @Override
                public void preferencesChanged() {
                    updateSettingsButton();
                }
            };
            dialog.showDialog();
        } else {
            if(view instanceof GPivot) {
                GPivot pivotTable = (GPivot) view;
                pivotTable.switchSettings();
                updateSettingsButton();
            }
        }
    }

    private void updateSettingsButton() {
        if(settingsButton != null) {
            if (view instanceof GGridTable) {
                GGridTable gridTable = (GGridTable) view;
                settingsButton.showBackground(gridTable.hasUserPreferences() || gridTable.generalPreferencesSaved() || gridTable.userPreferencesSaved());
            } else if (view instanceof GPivot) {
                GPivot pivotTable = (GPivot) view;
                settingsButton.showBackground(pivotTable.isSettings());
            }
        }
    }
}
