package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;

import java.util.List;

// a TABLE whose rows can be shown as a list of several kinds - a grid, a pivot, a map, a calendar, a custom view
// (GPlatformGridController), as it gives itself to its toolbar (GGridController.getListViewController): what the
// toolbar's view switch shows and asks of it, its buttons or a view's member
public interface GListViewController {
    // the kinds the design gives it, in the order the switch has them
    List<GListViewType> getListViews();
    // ... the one it shows now
    GListViewType getListView();
    // ... and another shown instead
    void changeListView(GListViewType view);
}
