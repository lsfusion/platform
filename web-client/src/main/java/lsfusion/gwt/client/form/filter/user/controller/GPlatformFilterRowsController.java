package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.core.client.Scheduler;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GUserFilter;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.view.Column;

import java.util.HashMap;
import java.util.Map;

// the platform's ROWS of a table's user filters, of the kind of what it draws them for - the FILTERS box
// (GPlatformFiltersController), a FILTER (GPlatformFilterController) -, each made where the projection answers that the
// platform draws it (GReactFormData.createFiltersControllers): a view of the model (GUserFiltersController), each row
// an editor of the entry it shows - what is changed in it goes into the model (edit), what the model has comes back
// into it (show) -, the rows of what a write sent marked loading till the answer. Driven with the table, with the
// others of the table's (GPlatformFilterRows): built with the table's first changes - the columns are known then
// (update) -, shown and hidden with it, let go of with the form, their selectors and delete buttons shown with the
// filter controls (GFilterControlsToggle)
public abstract class GPlatformFilterRowsController implements GFilterConditionView.UIHandler, GUserFiltersController.View {
    protected final GFormController form;
    protected final GTableController rows; // what draws the table's rows: the columns, the column a condition is on, the focus
    protected final GUserFiltersController userFilters;
    private final Map<Column, String> columns = new HashMap<>();

    // the entry each row shows
    private final NativeHashMap<GFilterConditionView, GUserFilter> rowEntries = new NativeHashMap<>();
    // what the rows are shown with: the filter controls - their selectors and delete buttons -, and the table - a row
    // made later too (addRow)
    private boolean controlsVisible;
    private boolean tableVisible = true;
    // the rows built, with the table's first changes (update): nothing is drawn before
    protected boolean built;

    protected GPlatformFilterRowsController(GFormController form, GTableController rows, GUserFiltersController userFilters) {
        this.form = form;
        this.rows = rows;
        this.userFilters = userFilters;
    }

    // what draws the rows has changed, once per delta: the columns a condition can be on, read anew, and - the first
    // time - the rows built (build), the model drawn
    void update() {
        columns.clear();
        for (Pair<Column, String> column : rows.getFilterColumns())
            columns.put(column.first, column.second);

        if (!built) {
            built = true;
            build();
            changed(null);
        }
    }
    // ... built the first time: a FILTER's row; the box's rows are its entries', drawn as the model is
    protected void build() {
    }
    // ... and the column a condition goes on: the property's - the table's current one, with no property -, on the
    // column given, else the one the table is on
    static Column getActualColumn(GTableController rows, GPropertyDraw property, GGroupObjectValue columnKey) {
        GPropertyDraw actualProperty = property;
        GGroupObjectValue actualColumnKey = columnKey;

        if (actualProperty == null) {
            actualProperty = rows.getSelectedFilterProperty();
            if (actualProperty != null) {
                actualColumnKey = rows.getSelectedColumnKey();
            }
        }
        // no column - a grid with no columns yet - is the column of the current objects of the column groups, as the
        // server takes it and reports it back
        return new Column(actualProperty, actualColumnKey != null ? actualColumnKey : GGroupObjectValue.EMPTY);
    }

    // ===== THE ROWS: one made for an entry - its condition's component in the layout, the FILTER itself or one the box
    // holds -, as the table and the controls are shown; bound to another entry - a slot's, which shows another one -:
    // its open editor cancelled first, so it commits into nothing it no longer shows, and its loading mark taken off,
    // that one's; let go of - its editor and its mark the same way
    protected GFilterConditionView addRow(GPropertyFilter condition, GUserFilter entry) {
        GFilterConditionView row = new GFilterConditionView(condition, form, rows, this, () -> columns, controlsVisible);
        form.getFormLayout().addBaseComponent(condition.filter, row.initView(), null);
        if (!tableVisible)
            GwtClientUtils.setGridVisible(row, false);
        rowEntries.put(row, entry);
        return row;
    }
    protected void bind(GFilterConditionView row, GUserFilter entry) {
        GUserFilter bound = rowEntries.get(row);
        if (bound != entry) {
            if (bound != null) {
                row.cancelEditing();
                form.clearLoading(row);
            }
            rowEntries.put(row, entry);
        }
    }
    protected void removeRow(GFilterConditionView row) {
        row.cancelEditing();
        form.clearLoading(row);
        rowEntries.remove(row);
    }
    // ... and the model's into each of them: its entry's condition, whether a condition of its group follows - its
    // junction shown then -, applied as its entry is; the rows of what a write sent marked loading till the answer
    protected void showRows(GUserFiltersController.Change change) {
        rowEntries.foreachEntry((view, entry) -> {
            view.show(entry.condition, entry.allowNull, entry.confirmed);
            view.setLast(userFilters.isLast(entry));
            view.setApplied(entry.applied);
        });
        if (change != null && change.requestIndex >= 0)
            for (GUserFilter sent : change.sent) {
                GFilterConditionView row = getRow(sent);
                if (row != null)
                    form.setLoading(row, change.requestIndex);
            }
    }

    // ... the row of an entry, if one of these shows it - a write's loading mark, F3 and typing over the panel put a
    // value into it (GPlatformFilterInputController.getRow)
    GFilterConditionView getRow(GUserFilter entry) {
        Result<GFilterConditionView> row = new Result<>();
        rowEntries.foreachEntry((view, shown) -> {
            if (shown == entry)
                row.set(view);
        });
        return row.result;
    }

    // ===== THE COMMANDS of the rows, and of the panel's input over them: the apply - what waits sent, as Enter in a
    // value cell does -, the reset - Escape in a row's selectors, Shift+F3: every condition off, sent at once -, the
    // focus back to the table's rows
    void applyConditions() {
        userFilters.apply();
        focusRows();
    }
    public void resetConditions() {
        userFilters.reset();
        focusRows();
    }
    private void focusRows() {
        Scheduler.get().scheduleDeferred(() -> rows.focusFirstWidget(FocusUtils.Reason.APPLYFILTER));
    }

    // ===== WHAT A ROW REPORTS (GFilterConditionView.UIHandler): its condition taken into its entry - written, sent at
    // once but in the manual apply mode, where its value takes the focus instead -, another property prepared, its
    // deletion and Enter sent at once
    public void conditionChanged(GFilterConditionView view, boolean focusValue) {
        boolean manual = userFilters.isManual();
        userFilters.edit(rowEntries.get(view), view.getCondition(), view.allowNull, view.confirmed, true);
        if (!manual)
            focusRows();
        else if (focusValue)
            view.focusValueView(); // to be able to apply by pressing Enter (even if focus was somewhere else before)
    }
    public void propertyChanged(GFilterConditionView view) {
        userFilters.edit(rowEntries.get(view), view.getCondition(), view.allowNull, false, false);
    }
    public void removeCondition(GFilterConditionView view) {
        userFilters.remove(rowEntries.get(view));
        focusRows();
    }
    public void enterPressed(GFilterConditionView view) {
        applyConditions();
    }

    // ===== WITH THE TOGGLE: the rows' selectors and delete buttons shown with the filter controls, the rows shown and
    // hidden with the table, and let go of with the form - a row still on screen holds its value cell, which is
    // registered for the life of the page
    void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        rowEntries.foreachKey(view -> view.setControlsVisible(visible));
    }
    void setVisible(boolean visible) {
        tableVisible = visible;
        rowEntries.foreachKey(view -> GwtClientUtils.setGridVisible(view, visible));
    }
    void destroy() {
        rowEntries.foreachKey(GFilterConditionView::destroy);
    }
}
