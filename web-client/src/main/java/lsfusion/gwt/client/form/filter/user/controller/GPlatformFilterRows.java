package lsfusion.gwt.client.form.filter.user.controller;

import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GReactFormData;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

// what of a table's user filters THE PLATFORM draws, where its toolbar shows them, as the projection asks it
// (GReactFormData.PlatformFilters): the row of each FILTER it draws (GPlatformFilterController), whoever draws the box,
// and the rows of the FILTERS box, where it draws the box - its filter panel (GPlatformFiltersController) -, with the
// panel's input (GPlatformFilterInputController); each a view of the table's user filters. Made by the form for a
// table (GFormController.initializeTable); driven with the table as one - each FILTER's row first, as it takes its
// column, then the box's, which draw the entries left to them -, their selectors shown with the filter controls
// (GFilterControlsToggle.Rows)
public final class GPlatformFilterRows implements GReactFormData.PlatformFilters {
    private final GFormController form;
    private final GTableController rows; // what draws the table's rows
    private final GFilterControlsToggle toggle; // what the panel's input shows and hides the controls through

    private GPlatformFiltersController box;
    private final NativeSIDMap<GFilter, GPlatformFilterController> filters = new NativeSIDMap<>();

    public GPlatformFilterRows(GFormController form, GTableController rows, GFilterControlsToggle toggle) {
        this.form = form;
        this.rows = rows;
        this.toggle = toggle;
    }

    // ===== WHAT IT DRAWS, as the projection answers that it does: a FILTER's row, the box's rows with the panel's
    // input - the box's a view of the model as it is made
    public GUserFiltersController.View addFilter(GFilter filter, GUserFiltersController userFilters) {
        GPlatformFilterController rows = new GPlatformFilterController(form, this.rows, filter, userFilters);
        filters.put(filter, rows);
        return rows;
    }
    public GFilterInputController addBox(GContainer filtersContainer, GUserFiltersController userFilters) {
        box = new GPlatformFiltersController(form, rows, filtersContainer, userFilters);
        userFilters.addView(box);
        return new GPlatformFilterInputController(rows, toggle, userFilters, this);
    }

    // ===== WITH THE TABLE AND THE CONTROLS (GFilterControlsToggle.Rows): what draws the rows has changed, once per
    // delta - the rows built the first time -; the table shown or hidden; the form closing; the selectors shown with
    // the controls; whether it draws a row at all - a FILTER's, or the box's, the box laid out -, which the toolbar's
    // button is there for, and whether the box has one now - the controls hide once the last row is gone
    public void update() {
        filters.foreachValue(GPlatformFilterRowsController::update);
        if (box != null)
            box.update();
    }
    public void setVisible(boolean visible) {
        filters.foreachValue(rows -> rows.setVisible(visible));
        if (box != null)
            box.setVisible(visible);
    }
    public void destroy() {
        filters.foreachValue(GPlatformFilterRowsController::destroy);
        if (box != null)
            box.destroy();
    }
    public void setControlsVisible(boolean visible) {
        filters.foreachValue(rows -> rows.setControlsVisible(visible));
        if (box != null)
            box.setControlsVisible(visible);
    }
    public boolean drawsRows() {
        return !filters.isEmpty() || box != null && box.drawsRows();
    }
    public boolean hasBoxRows() {
        return box != null && box.hasRows();
    }

    // ===== WHERE A VALUE GOES (GPlatformFilterInputController): the box's rows, which the panel's input is the input
    // over; a FILTER's row, if the platform draws it - none before the table's first changes -, and whether it draws it
    // - else React draws it, the projection answered
    GPlatformFiltersController getBox() {
        return box;
    }
    GFilterConditionView getFilterRow(GFilter filter) {
        GPlatformFilterController rows = filters.get(filter);
        return rows != null ? rows.getRow() : null;
    }
    boolean drawsRowOf(GFilter filter) {
        return filters.containsKey(filter);
    }
}
