package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GUserFilter;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.filter.user.view.GFilterEditors;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

import java.util.*;

// the platform's FILTERS of a group, or of a tree for all its groups: its filter panel - what draws the table's user
// filters where the platform draws the FILTERS box, and what takes their input there (GFilterInputController). It is a
// view of them (GUserFiltersController): a row for each entry the box shows, in the model's order, and a row for each
// FILTER the platform draws - a slot, which shows what the model gives it (getSlotEntry) wherever the design puts it. A
// row is an editor of its entry: what is changed in it goes into the model (edit), what the model has comes back into
// it (show). The input works on the model too: F3, typing and the controls' add PREPARE a value - put into the first
// row on the column, or into a new one, its editor started -, sent with the next write; F3 into a FILTER React draws
// writes it, there being no editor. Its rows' selectors and delete buttons are what the toolbar's button shows
// (GFilterEditors)
public final class GPlatformFiltersController extends GFilterInputController implements GFilterConditionView.UIHandler, GFilterEditors, GUserFiltersController.View {
    private final GFormController form;
    private final GContainer filtersContainer;
    private final boolean hasFiltersContainer;
    private final List<GFilter> filters; // the table's FILTERs, as the design declares them
    private final GUserFiltersController userFilters;
    private final Map<Column, String> columns = new HashMap<>();

    // the rows: each FILTER's the platform draws - its slot's -, and each of the box's, by its entry; and the entry
    // each row shows
    private final NativeSIDMap<GFilter, GFilterConditionView> slotRows = new NativeSIDMap<>();
    private NativeHashMap<GUserFilter, GFilterConditionView> boxRows = new NativeHashMap<>();
    private final NativeHashMap<GFilterConditionView, GUserFilter> rowEntries = new NativeHashMap<>();
    // the rows built, with the form's first changes (update): nothing is drawn before
    private boolean built;

    public GPlatformFiltersController(GFormController form, GTableController rows, GContainer filtersContainer, List<GFilter> filters, GUserFiltersController userFilters, GFilterControlsToggle toggle) {
        super(rows, toggle);
        this.form = form;
        this.filtersContainer = filtersContainer;
        this.filters = filters;
        this.userFilters = userFilters;
        this.hasFiltersContainer = form.formLayout.getContainerView(filtersContainer) != null;

        userFilters.addView(this);
        toggle.bind(this, this, userFilters); // its commands, and its editors - once what its controls ask of them is set
    }

    // what draws the rows has changed, once per delta (GFilterInputController.update): the columns a condition can be
    // on, read anew, and - the first time - the row of each FILTER the platform draws, on the column the table is on
    public void update() {
        columns.clear();
        for (Pair<Column, String> column : rows.getFilterColumns())
            columns.put(column.first, column.second);

        if (!built) {
            built = true;
            for (GFilter filter : filters)
                if (filter.container != null) { // one the design removes has none
                    filter.fixed = true;
                    if (!isDrawnByReact(filter)) {
                        Column column = getActualColumn(rows, filter.property, rows.getSelectedColumnKey());
                        userFilters.setSlotColumn(filter, column.columnKey);
                        GFilterConditionView row = new GFilterConditionView(new GPropertyFilter(filter, column.property.groupObject, column.columnKey, null, null),
                                form, rows, this, () -> columns, toggle.isVisible());
                        form.getFormLayout().addBaseComponent(filter, row.initView(), null);
                        slotRows.put(filter, row);
                    }
                }
            changed(null);
        }
    }

    // a FILTER React draws over this panel - in a React view, or with one of its own (GReactFormData.FilterPart): a
    // slot the panel draws no row of
    private static boolean isDrawnByReact(GFilter filter) {
        return filter.isReact() || filter.isReactDrawn();
    }

    // ===== THE MODEL DRAWN: each slot's row showing what the model gives it, the box's rows one for each entry it
    // shows, in the model's order - a row kept while its entry is, so is its editor, a new one put at the end of the
    // box; the rows kept in another order, all of them made anew -, each row's junction shown where a condition of its
    // group follows its entry, applied as its entry is; the rows of what a write sent marked loading till the answer.
    // The controls hidden once the last row is gone
    public void changed(GUserFiltersController.Change change) {
        if (!built)
            return;
        boolean hadConditions = hasConditions();

        slotRows.foreachEntry((slot, row) -> bind(row, userFilters.getSlotEntry(slot)));

        boolean relayout = false;
        if (hasFiltersContainer) {
            List<GUserFilter> free = userFilters.getFreeEntries();
            List<GUserFilter> kept = new ArrayList<>();
            boxRows.foreachKey(kept::add);
            kept.retainAll(free);
            if (!kept.equals(free.subList(0, kept.size()))) // not the first ones in their order
                kept.clear();
            NativeHashMap<GUserFilter, GFilterConditionView> drawn = new NativeHashMap<>();
            for (GUserFilter entry : kept)
                drawn.put(entry, boxRows.remove(entry));
            relayout = !boxRows.isEmpty() || kept.size() < free.size();
            boxRows.foreachValue(this::removeBoxRow);
            for (GUserFilter entry : free.subList(kept.size(), free.size()))
                drawn.put(entry, addBoxRow(entry));
            boxRows = drawn;
        }

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

        if (relayout)
            form.getFormLayout().update(-1);
        if (hadConditions && !hasConditions() && toggle.isVisible())
            setControlsVisible(false);
    }
    // a slot's row bound to the entry it shows now - another one: its open editor cancelled first, so it commits into
    // nothing it no longer shows, and its loading mark taken off, that one's
    private void bind(GFilterConditionView row, GUserFilter entry) {
        GUserFilter bound = rowEntries.get(row);
        if (bound != entry) {
            if (bound != null) {
                row.cancelEditing();
                form.clearLoading(row);
            }
            rowEntries.put(row, entry);
        }
    }
    // a row of the box's for an entry - a component of its own, in the box
    private GFilterConditionView addBoxRow(GUserFilter entry) {
        GPropertyFilter condition = entry.condition;
        GPropertyFilter own = new GPropertyFilter(new GFilter(condition.column.property), condition.groupObject, condition.column.columnKey, null, null);
        filtersContainer.add(own.filter);
        GFilterConditionView row = new GFilterConditionView(own, form, rows, this, () -> columns, toggle.isVisible());
        form.getFormLayout().addBaseComponent(own.filter, row.initView(), null);
        rowEntries.put(row, entry);
        return row;
    }
    // ... and one whose entry is gone: its open editor cancelled, its loading mark taken off, its component out of the
    // box, its value cell let go of
    private void removeBoxRow(GFilterConditionView row) {
        row.cancelEditing();
        form.clearLoading(row);
        GFilter component = row.getCondition().filter;
        form.getFormLayout().removeBaseComponent(component);
        filtersContainer.removeFromChildren(component);
        row.destroy();
        rowEntries.remove(row);
    }
    // the row of an entry: the box's, or the slot's that shows it
    private GFilterConditionView getRow(GUserFilter entry) {
        GFilterConditionView row = boxRows.get(entry);
        if (row != null)
            return row;
        GFilter slot = userFilters.getSlotOf(entry);
        return slot != null ? slotRows.get(slot) : null;
    }
    // whether the panel has a row at all - the box's, or a FILTER's, React drawing it or not -, which the controls are
    // hidden without
    private boolean hasConditions() {
        if (!boxRows.isEmpty())
            return true;
        for (GFilter filter : filters)
            if (filter.container != null)
                return true;
        return false;
    }

    // ===== THE INPUT
    // the table's toolbar button, or a key that starts a condition: the controls shown or hidden - shown with no row of
    // the box's, one is added
    @Override
    public void toggleControlsVisible() {
        super.toggleControlsVisible();

        if (toggle.isVisible() && boxRows.isEmpty() && hasFiltersContainer)
            addCondition();
    }

    private static Column getActualColumn(GTableController rows, GPropertyDraw property, GGroupObjectValue columnKey) {
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

    // the controls' add: a new row on the current column - its value the cell's -, its editor started
    public void addCondition() {
        addCondition(GKeyStroke.createAddUserFilterKeyEvent());
    }

    public void addCondition(Event keyEvent) {
        addCondition(keyEvent, false);
    }

    public void addCondition(Event keyEvent, boolean replace) {
        addCondition(keyEvent, replace, false);
    }

    // F3 (checkExistingFilter): the cell's value put into the first row on its column, else into a new one; Alt+F3
    // (replace): the conditions reset, then a new row; either its editor started. A FILTER's row React draws is a row
    // as any other - the first on the column, or the one a new row would be, showing none, it takes the value
    // (putCellValue)
    public void addCondition(Event keyEvent, boolean replace, boolean checkExistingFilter) {
        Column column = getActualColumn(rows, null, null);
        if (column.property == null)
            return;
        if (replace)
            userFilters.reset();
        GUserFilter first = checkExistingFilter ? userFilters.getFirstEntry(column) : null;
        GFilter react = getReactFilter(column, first);
        if (react != null)
            putCellValue(react, column);
        else if (first != null)
            prepareCellValue(first, column, keyEvent);
        else
            addRow(column, keyEvent);
    }
    // typing in a column: the key typed starts the editor of its first row - one React draws aside -, else of a new
    // one; none where the FILTER React draws on the column would take it, there being no editor here to type in
    public void quickEditFilter(Event keyEvent, GPropertyDraw property, GGroupObjectValue columnKey) {
        Column column = getActualColumn(rows, property, columnKey);
        if (getReactFilter(column, null) != null)
            return;
        GUserFilter first = userFilters.getFirstEntry(column);
        GFilter slot = first != null ? userFilters.getSlotOf(first) : null;
        if (first != null && (slot == null || !isDrawnByReact(slot)))
            prepareCellValue(first, column, keyEvent);
        else
            addRow(column, keyEvent);
    }
    // the FILTER React draws that a value put on a column goes into - drawn there, with no editor here -: the one the
    // first entry on the column is of, else the one on the column showing no condition, which an entry made active
    // there - a new row's, an emptied one's - would be shown by; null for none
    private GFilter getReactFilter(Column column, GUserFilter first) {
        GFilter filter = first != null ? userFilters.getSlotOf(first) : null;
        if (filter != null && isDrawnByReact(filter))
            return filter;
        filter = userFilters.getFilter(column);
        return filter != null && isDrawnByReact(filter) && !userFilters.getSlotEntry(filter).isActive() ? filter : null;
    }
    // the cell's value - escaped by the comparison it is put in, where the cell shows the value it filters by
    // (differentValue) -, put into a row, prepared, its editor started
    private void prepareCellValue(GUserFilter entry, Column column, Event keyEvent) {
        if (!column.property.differentValue)
            userFilters.prepareValue(entry, readCellValue(column, entry.condition.compare));
        GFilterConditionView row = getRow(entry);
        if (row != null) {
            row.focusValueView(); // to leave focus on value with manual apply mode and be able to apply on Enter
            row.startEditing(keyEvent);
        }
    }
    // ... and into a new row of the box's, compared the default way, its value taking the focus and the key starting
    // its editor - once a batch is sent, where a view's member added it in one: a focus sends a request of the
    // platform's own, a property made active, which a batch does not carry
    private void addRow(Column column, Event keyEvent) {
        PValue value = column.property.differentValue ? null : readCellValue(column, column.property.getDefaultCompare());
        GUserFilter entry = userFilters.add(new GPropertyFilter(new GFilter(column.property), column.property.groupObject, column.columnKey, value, null));
        GFilterConditionView row = entry != null ? getRow(entry) : null;
        if (row == null)
            return;
        Scheduler.ScheduledCommand focus = () -> {
            row.onAdd(true);
            if (keyEvent != null)
                row.startEditing(keyEvent);
        };
        if (form.isBatching())
            Scheduler.get().scheduleDeferred(focus);
        else
            focus.execute();
    }
    // the cell's value put into a FILTER's row React draws: its comparison, negation and junction kept, written - React
    // drawing it, there is no editor -; an empty cell's none takes its condition off, as the platform's row of a
    // FILTER, which allows no NULL, takes none; nothing on no row (an empty grid), which has no value to put
    private void putCellValue(GFilter slot, Column column) {
        if (rows.getSelectedRow() < 0 || column.property.differentValue)
            return;
        GPropertyFilter base = userFilters.getSlotEntry(slot).condition;
        PValue value = readCellValue(column, base.compare);
        if (value == null)
            userFilters.clearFilter(slot);
        else
            userFilters.changeFilter(slot, base.restated(value, base.negation, base.compare, base.junction));
    }
    private PValue readCellValue(Column column, GCompare compare) {
        return PValue.escapeSeparator(rows.getSelectedValue(column.property, column.columnKey), compare);
    }

    // the controls' apply - what waits sent, as Enter in a value cell does -, and their reset, Escape in a row's
    // selectors, Shift+F3: every condition off, sent at once; the focus back to the rows
    public void applyConditions() {
        userFilters.apply();
        focusRows();
    }
    public void resetConditions() {
        userFilters.reset();
        focusRows();
    }
    // ... whether there is anything to reset - a row of the box's, a FILTER's applied, or what the server still filters
    // by, a change of it waiting -, which Escape in the grid asks
    public boolean hasConditionsToReset() {
        if (!boxRows.isEmpty() || userFilters.isPending())
            return true;
        for (GFilter filter : filters) {
            GUserFilter entry = userFilters.getSlotEntry(filter);
            if (entry != null && entry.applied)
                return true;
        }
        return false;
    }
    public boolean hasFiltersContainer() {
        return hasFiltersContainer;
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

    // ===== THE EDITORS, the toolbar's button showing and hiding them (GFilterEditors): the rows' selectors and their
    // delete buttons
    public void setConditionsControlsVisible(boolean visible) {
        rowEntries.foreachKey(view -> view.setControlsVisible(visible));
    }

    @Override
    public void setVisible(boolean visible) {
        rowEntries.foreachKey(conditionView -> GwtClientUtils.setGridVisible(conditionView, visible));
        super.setVisible(visible);
    }

    // the form is closing: a row still on screen holds its value cell, and that cell is registered for the life of the
    // page
    public void destroy() {
        rowEntries.foreachKey(GFilterConditionView::destroy);
    }
}
