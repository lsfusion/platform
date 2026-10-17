package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;

// ... of a LIST's group - TOOLBARSYSTEM(g): besides what its rows give it, the view the table shows and the state of
// the group's update
public interface GGridToolbarController extends GToolbarController<GGridController> {
    // the view the table shows, which it shows
    void showView(GListViewType view);

    // whether the server holds changes of the group back - null: it says nothing new -, as the form tells it with
    // each delta (GFormController.update)
    void updateState(Boolean updateState);
}
