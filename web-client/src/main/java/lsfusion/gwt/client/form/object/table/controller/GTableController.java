package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GFilterInputController;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.List;

// what draws a TABLE's rows - of a list's group's grid, or of a tree, one for all its groups (GGridController,
// GTreeController) -, whoever draws them, chosen once (GReactFormData.createGridController, createTreeController): the
// platform's table (GPlatformTableController), or the rows React draws (GReactFormData.TablePart). First what the
// filter input controller of the table's FILTERS asks of them (GFilterInputController): the cell a new condition starts
// on - its column, and the row the table is on, -1 for none - and that cell's value, and its conditions stated as the
// list the box's groups are filtered by; and what the platform's panel asks besides: the columns a condition can be on,
// the group its bindings are of, and the focus taken back once they are applied
public interface GTableController {
    GGroupObject getSelectedGroupObject();
    GPropertyDraw getSelectedFilterProperty();
    GGroupObjectValue getSelectedColumnKey();
    int getSelectedRow();
    PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey);
    List<Pair<Column, String>> getFilterColumns();

    long changeFilter(ArrayList<GPropertyFilter> conditions);
    boolean focusFirstWidget(FocusUtils.Reason reason);

    // ... and the filter input controller itself, given to the rows once it is made over them
    // (GFormController.initializeTable), and told that they have changed, once per delta
    // (GFilterInputController.update): by a table of the platform's, which gives it its keys too, by the projection
    // where React draws the rows
    void bindFilter(GFilterInputController filter);

    // ... and what the form takes of them for the table's groups, as it sets the table up: the form's controller of
    // each group - a table of the platform's itself, the group's rows' part where React draws the rows -, and what
    // shows the groups' user orders - a table of the platform's, whose header shows them, the part of its own beside
    // the rows React draws - (GFormController.initializeTable)
    GGroupController getGroupController(GGroupObject group);
    GUserOrdersController getUserOrdersController();

    // ===== what the form asks of every table's rows alike, whoever draws them (GFormController.tableControllers): the
    // delta applied, its index and its changes given - a table of the platform's draws it; the rows React draws only
    // tell the filter input controller given to them, the projection publishing what React draws -; the form closing -
    // what either keeps for the life of the page let go of: a table's views, the filter input controller -; the user
    // preferences of the table's views, a platform grid's only; and the platform's controller of the table's list
    // properties, where it draws their columns itself - none where React draws the rows
    void update(long requestIndex, GFormChanges fc);
    void destroy();
    default void addUserPreferences(List<GGroupObjectUserPreferences> user, List<GGroupObjectUserPreferences> general) {
    }
    default GPlatformPropertyController getPlatformPropertyController() {
        return null;
    }
}
