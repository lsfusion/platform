package lsfusion.gwt.client.form.object.table.grid.controller;

import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.object.table.controller.GListViewController;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

// what draws the rows of a group's GRID - GRID(g), whichever list view the platform's table shows in it, a grid, a
// pivot, a map, a calendar or a custom view -, as the group's filter input controller (GTableController) and its
// toolbar ask them, whoever draws them (GReactFormData.createGridController): the platform's table
// (GPlatformGridController), which gives itself to the toolbar as it is made, or the rows React draws there
// (GReactFormData.GridTablePart), which the projection gives it as it makes them (GGridToolbarController.addRows) - as
// a tree's rows (GTreeController)
public interface GGridController extends GTableController {
    // its buttons on the platform's toolbar, in their order, which only the platform's toolbar asks for: a table's, its
    // view switch first (GPlatformGridController.addToolbarButtons), or what the rows React draws have, which needs no
    // table (Toolbar.addButtonsWithoutTable)
    void addToolbarButtons(Toolbar toolbar);
    // ... and the views its rows can be shown as, which a view's part of the toolbar switches: a table's own, none
    // where React draws the rows
    GListViewController getListViewController();

    // the platform's toolbar, as the rows put their buttons on it (GPlatformGridToolbarController): a table's view
    // switch, a tool of the rows' own, the count of the group's rows - a button for a group of the rows' -, the export,
    // handed back to whoever enables it, and what needs no table
    interface Toolbar {
        void addViewSwitch(GListViewController table);
        void add(Widget tool);
        Widget createCountButton();
        FocusWidget addExport(Runnable export);
        void addButtonsWithoutTable();
    }
}
