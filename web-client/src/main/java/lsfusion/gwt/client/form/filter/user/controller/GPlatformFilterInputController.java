package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GUserFilter;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;

// the platform's INPUT where it draws the FILTERS box - its filter panel (GPlatformFiltersController): F3, typing and
// the controls' add PREPARE a value - put into the first row on the column, the box's or that of a FILTER the platform
// draws, or into a new one, its editor started -, sent with the next write; F3 into a FILTER React draws writes it,
// there being no editor
public final class GPlatformFilterInputController extends GFilterInputController {
    // all the rows the platform draws - the box's, which it is the input over (getBox), and the FILTERs' -, where F3
    // and typing put a value
    private final GPlatformFilterRows platformRows;

    public GPlatformFilterInputController(GTableController rows, GFilterControlsToggle toggle, GUserFiltersController userFilters, GPlatformFilterRows platformRows) {
        super(rows, toggle, userFilters);
        this.platformRows = platformRows;
    }

    // the table's toolbar button, or a key that starts a condition: the controls shown or hidden - shown with no row of
    // the box's, one is added
    @Override
    public void toggleControlsVisible() {
        super.toggleControlsVisible();

        if (toggle.isVisible() && !platformRows.hasBoxRows() && platformRows.getBox().drawsRows())
            addCondition();
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
    // (replace): the conditions reset, then a new row; either its editor started. A FILTER React draws is a row as any
    // other - the first on the column, or the one a new row would be, showing none, it takes the value (putCellValue)
    public void addCondition(Event keyEvent, boolean replace, boolean checkExistingFilter) {
        Column column = GPlatformFilterRowsController.getActualColumn(rows, null, null);
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
        Column column = GPlatformFilterRowsController.getActualColumn(rows, property, columnKey);
        if (getReactFilter(column, null) != null)
            return;
        GUserFilter first = userFilters.getFirstEntry(column);
        GFilter slot = first != null ? userFilters.getSlotOf(first) : null;
        if (first != null && (slot == null || platformRows.drawsRowOf(slot)))
            prepareCellValue(first, column, keyEvent);
        else
            addRow(column, keyEvent);
    }
    // the FILTER React draws that a value put on a column goes into - one the platform draws no row of, the projection
    // answered, with no editor here -: the one the first entry on the column is of, else the one on the column showing
    // no condition, which an entry made active there - a new row's, an emptied one's - would be shown by; null for none
    private GFilter getReactFilter(Column column, GUserFilter first) {
        GFilter filter = first != null ? userFilters.getSlotOf(first) : null;
        if (filter != null && !platformRows.drawsRowOf(filter))
            return filter;
        filter = userFilters.getFilter(column);
        return filter != null && !platformRows.drawsRowOf(filter) && !userFilters.getSlotEntry(filter).isActive() ? filter : null;
    }
    // the cell's value - escaped by the comparison it is put in, where the cell shows the value it filters by
    // (differentValue) -, put into a row, prepared, its editor started: the row of the platform's that shows its entry
    // then - the box's, a FILTER's -, if any
    private void prepareCellValue(GUserFilter entry, Column column, Event keyEvent) {
        if (!column.property.differentValue)
            userFilters.prepareValue(entry, readCellValue(column, entry.condition.compare));
        GFilterConditionView row = getRow(entry);
        if (row != null) {
            row.focusValueView(); // to leave focus on value with manual apply mode and be able to apply on Enter
            row.startEditing(keyEvent);
        }
    }
    // ... and into a new row - the box's, or a FILTER's on the column showing none -, compared the default way, its
    // value taking the focus and the key starting its editor - once a batch is sent, where a view's member added it in
    // one: a focus sends a request of the platform's own, a property made active, which a batch does not carry
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
        if (platformRows.getBox().isBatching())
            Scheduler.get().scheduleDeferred(focus);
        else
            focus.execute();
    }
    // the platform's row an entry is shown in, if any: that of the FILTER whose slot shows it, else the box's
    private GFilterConditionView getRow(GUserFilter entry) {
        GFilter slot = userFilters.getSlotOf(entry);
        return slot != null ? platformRows.getFilterRow(slot) : platformRows.getBox().getRow(entry);
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

    // the controls' apply and reset, as any input's - the focus back to the rows then, as the panel's rows give it
    @Override
    public void applyConditions() {
        platformRows.getBox().applyConditions();
    }
    @Override
    public void resetConditions() {
        platformRows.getBox().resetConditions();
    }
    // the controls' add, which the design has the FILTERS box for: where the panel draws rows - the box laid out
    public boolean hasFiltersContainer() {
        return platformRows.getBox().drawsRows();
    }
}
