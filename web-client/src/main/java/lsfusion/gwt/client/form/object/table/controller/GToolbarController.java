package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.form.filter.user.controller.GFilterInputController;

// the form's controller of a TOOLBAR - TOOLBARSYSTEM -, a list's group's (GGridToolbarController) or a tree's, one for
// all its groups, of the kind of whoever draws it, chosen once (GReactFormData.createGridToolbarController,
// createTreeToolbarController): the platform's buttons (GPlatformGridToolbarController,
// GPlatformTreeToolbarController), or a view's part of the group or of the tree, whose members are the buttons
// (GReactFormData.GridToolbarPart, TreeToolbarPart). What draws the rows and the filter controller give each kind the
// same, and each has it its own way; the state its buttons show each kind keeps: the filter controls shown, which the
// filter controller tells it of, and a group's update, which the form tells it of. Here is what a toolbar has whoever
// owns it, over the rows of its table's kind (R)
public interface GToolbarController<R extends GTableController> {
    // what draws the rows, given to it as it is made, whoever draws them - the platform's table gives itself, the
    // projection the rows React draws: a grid's rows give the platform's toolbar their buttons and a view's part their
    // views (GGridController); a tree's are what the toolbar opens it through - the platform's buttons, the two that
    // open it and the export, and a view's part's members, the two that open it where the rows say so (GTreeController)
    void addRows(R rows);

    // the filter button, which shows and hides its controls; and whether they are shown, which it
    // shows
    void addFilterToggle(GFilterInputController filter);
    void showFilterControls(boolean shown);

    // the table shown or hidden - a grid with no columns hides itself -, which the table tells whichever toolbar it
    // has, holding it by this type: the platform's toolbar hides with the table; a view's part has nothing of the
    // platform's to hide, and what a view draws is the view's to show, which does not follow the platform's table
    void setVisible(boolean visible);
}
