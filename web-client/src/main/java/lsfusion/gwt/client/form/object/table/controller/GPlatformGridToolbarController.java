package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GCountQuantityButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButtonGroup;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;

// ... of a LIST's group - TOOLBARSYSTEM(g): the buttons a group's toolbar has whatever draws the rows, the count of its
// rows and its manual update, which need no table, and those of whoever draws them, which give them as they are made
// (addRows) - a table's, in their order, its view switch among them, or what the rows React draws have, which needs no
// table -, the filter button after the view switch; and the state of the group's update, which the form tells it of
public final class GPlatformGridToolbarController extends GPlatformToolbarController<GGridController, GGrid> implements GGridToolbarController, GGridController.Toolbar {

    public GPlatformGridToolbarController(GFormController form, GGrid grid) {
        super(form, grid);
    }

    // ===== what draws the rows puts on it, as it is made: its buttons, in their order - asked for them here ... -,
    // and after them the group's manual update, whoever draws the rows
    public void addRows(GGridController rows) {
        rows.addToolbarButtons(this);
        if (table.toolbar.showManualUpdate)
            addManualUpdate();
    }
    // ... which, where React draws the rows, are what needs no table (GReactFormData.GridTablePart): the count and the
    // export - the server's report of the rows -, not a table's view switch, settings or sum; the filter button goes
    // before them all the same
    public void addButtonsWithoutTable() {
        if (table.toolbar.showCountQuantity) {
            GToolbarButtonGroup calculateButtonGroup = new GToolbarButtonGroup();
            calculateButtonGroup.add(createCountButton());
            add(calculateButtonGroup);
        }
        addReportExport();
    }

    // ===== a table's view switch: a button for each view the design gives its rows, pressed for the one it shows - the
    // first of the table's buttons (GPlatformGridController.addToolbarButtons)
    private final NativeHashMap<GListViewType, GToolbarButton> viewButtons = new NativeHashMap<>();

    public void addViewSwitch(GListViewController table) {
        GToolbarButtonGroup viewButtonGroup = new GToolbarButtonGroup();
        for (GListViewType listView : table.getListViews()) {
            GToolbarButton viewButton = new GToolbarButton(getViewImage(listView), getViewCaption(listView)) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> table.changeListView(listView);
                }
            };
            viewButtons.put(listView, viewButton);
            viewButtonGroup.add(viewButton);
        }
        add(viewButtonGroup);
    }

    public void showView(GListViewType shown) {
        viewButtons.foreachEntry((view, button) -> button.showBackground(view == shown));
    }

    private static StaticImage getViewImage(GListViewType listView) {
        switch (listView) {
            case PIVOT: return StaticImage.PIVOT;
            case CUSTOM: return StaticImage.CUSTOMVIEW;
            case MAP: return StaticImage.MAP;
            case CALENDAR: return StaticImage.CALENDAR;
            default: return StaticImage.GRID;
        }
    }
    private static String getViewCaption(GListViewType listView) {
        switch (listView) {
            case PIVOT: return messages.formGridPivotView();
            case CUSTOM: return messages.formGridCustomView();
            case MAP: return messages.formGridMapView();
            case CALENDAR: return messages.formGridCalendarView();
            default: return messages.formGridTableView();
        }
    }

    // ===== the filter button: after the view switch, where there is one, before the rest
    @Override
    protected int getFilterToggleIndex() {
        return viewButtons.isEmpty() ? 0 : 1;
    }

    // ===== the count of the group's rows: a button - one of the group's calculations, so whoever builds the toolbar
    // puts it in their group -, which shows the count it asks the server for in a popup (GFormController.countRecords)
    public GCountQuantityButton createCountButton() {
        return new GCountQuantityButton() {
            @Override
            public ClickHandler getClickHandler() {
                return event -> form.countRecords(table.groupObject, quantity -> showPopup(quantity.longValue()), error -> { });
            }
        };
    }

    // ===== the manual update of the group: a toggle, showing the group's update mode as the form keeps it
    // (GFormController.isManualUpdate), and the button that brings its rows up to date while it is on - enabled while
    // the server holds changes back (updateState)
    private GToolbarButton manualUpdateTableButton;
    private GToolbarButton forceUpdateTableButton;

    private void addManualUpdate() {
        GToolbarButtonGroup updateButtonGroup = new GToolbarButtonGroup();

        manualUpdateTableButton = new GToolbarButton(StaticImage.UPDATE, messages.formGridManualUpdate()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    boolean manual = !form.isManualUpdate(table.groupObject);
                    form.changeUpdateMode(table.groupObject, manual ? GUpdateMode.MANUAL : GUpdateMode.AUTO);
                    setUpdateMode(manual);
                };
            }
        };
        updateButtonGroup.add(manualUpdateTableButton);

        forceUpdateTableButton = new GToolbarButton(messages.formGridUpdate(), StaticImage.OK, messages.formGridUpdate()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> form.changeUpdateMode(table.groupObject, GUpdateMode.FORCE);
            }
        };
        updateButtonGroup.add(forceUpdateTableButton);

        add(updateButtonGroup);

        setUpdateMode(form.isManualUpdate(table.groupObject));
    }

    private void setUpdateMode(boolean manual) {
        if (manual) {
            forceUpdateTableButton.setVisible(true);
            forceUpdateTableButton.setEnabled(false);
        } else
            forceUpdateTableButton.setVisible(false);
        manualUpdateTableButton.showBackground(manual);
    }

    public void updateState(Boolean updateState) {
        if (updateState != null && forceUpdateTableButton != null)
            forceUpdateTableButton.setEnabled(updateState);
    }
}
