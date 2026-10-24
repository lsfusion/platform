package lsfusion.gwt.client.form.filter.user.controller;

import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.view.Column;

// the platform's row of a property's FILTER - FILTER(<property>) the platform draws, wherever the design puts it,
// whoever draws the FILTERS box -, a kind of its rows (GPlatformFilterRowsController): a slot of the table's user
// filters, it shows what the model gives it (GUserFiltersController.getSlotEntry) - the first active entry on its
// column, else its placeholder -, its row bound to that entry. Built with the table's first changes, on the column the
// table is on then - a property grouped in columns has its column only then -; a FILTER is never removed. React's kind
// of it is a part of the group of its property (GReactFormData.FilterPart), the two chosen where the platform's rows
// of a table are made (GReactFormData.createFiltersControllers)
public final class GPlatformFilterController extends GPlatformFilterRowsController {
    private final GFilter filter;
    private GFilterConditionView row;

    public GPlatformFilterController(GFormController form, GTableController rows, GFilter filter, GUserFiltersController userFilters) {
        super(form, rows, userFilters);
        this.filter = filter;
    }

    protected void build() {
        filter.fixed = true;
        Column column = getActualColumn(rows, filter.property, rows.getSelectedColumnKey());
        userFilters.setSlotColumn(filter, column.columnKey);
        row = addRow(new GPropertyFilter(filter, column.property.groupObject, column.columnKey, null, null), userFilters.getSlotEntry(filter));
    }

    // ... its row, none before the table's first changes (GPlatformFilterRows.getFilterRow)
    GFilterConditionView getRow() {
        return row;
    }

    // ===== THE MODEL DRAWN: its row bound to what its slot shows now, showing it
    public void changed(GUserFiltersController.Change change) {
        if (!built)
            return;
        bind(row, userFilters.getSlotEntry(filter));
        showRows(change);
    }
}
