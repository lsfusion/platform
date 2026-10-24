package lsfusion.gwt.client.form.filter.user.controller;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GUserFilter;
import lsfusion.gwt.client.form.filter.user.GUserFiltersController;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.util.ArrayList;
import java.util.List;

// the platform's rows of a table's FILTERS box - its filter panel -: a row for each entry the box shows, in the model's
// order (GUserFiltersController.getFreeEntries), a component of its own in the box. A kind of the platform's rows
// (GPlatformFilterRowsController), made where the platform draws the box, the panel's input working on it
// (GPlatformFilterInputController); none drawn where the design lays the box out nowhere. React's kind of it is the
// box's part (GReactFormData.FiltersPart)
public final class GPlatformFiltersController extends GPlatformFilterRowsController {
    private final GContainer filtersContainer; // the box it draws the rows of
    private final boolean hasFiltersContainer; // ... laid out

    private NativeHashMap<GUserFilter, GFilterConditionView> boxRows = new NativeHashMap<>();

    public GPlatformFiltersController(GFormController form, GTableController rows, GContainer filtersContainer, GUserFiltersController userFilters) {
        super(form, rows, userFilters);
        this.filtersContainer = filtersContainer;
        this.hasFiltersContainer = filtersContainer != null && form.formLayout.getContainerView(filtersContainer) != null;
    }

    // ===== THE MODEL DRAWN: a row for each entry the box shows, in the model's order - a row kept while its entry is,
    // so is its editor, a new one put at the end of the box; the rows kept in another order, all of them made anew -,
    // the form laid out anew once a row comes or goes
    public void changed(GUserFiltersController.Change change) {
        if (!built || !hasFiltersContainer)
            return;

        List<GUserFilter> free = userFilters.getFreeEntries();
        List<GUserFilter> kept = new ArrayList<>();
        boxRows.foreachKey(kept::add);
        kept.retainAll(free);
        if (!kept.equals(free.subList(0, kept.size()))) // not the first ones in their order
            kept.clear();
        NativeHashMap<GUserFilter, GFilterConditionView> drawn = new NativeHashMap<>();
        for (GUserFilter entry : kept)
            drawn.put(entry, boxRows.remove(entry));
        boolean relayout = !boxRows.isEmpty() || kept.size() < free.size();
        boxRows.foreachValue(this::removeBoxRow);
        for (GUserFilter entry : free.subList(kept.size(), free.size()))
            drawn.put(entry, addBoxRow(entry));
        boxRows = drawn;

        showRows(change);
        if (relayout)
            form.getFormLayout().update(-1);
    }
    // a row of the box's for an entry - a component of its own, in the box
    private GFilterConditionView addBoxRow(GUserFilter entry) {
        GPropertyFilter condition = entry.condition;
        GPropertyFilter own = new GPropertyFilter(new GFilter(condition.column.property), condition.groupObject, condition.column.columnKey, null, null);
        filtersContainer.add(own.filter);
        return addRow(own, entry, false);
    }
    // ... and one whose entry is gone: its component out of the box, its value cell let go of
    private void removeBoxRow(GFilterConditionView row) {
        removeRow(row);
        GFilter component = row.getCondition().filter;
        form.getFormLayout().removeBaseComponent(component);
        filtersContainer.removeFromChildren(component);
        row.destroy();
    }

    // whether it draws rows at all - the box laid out -, and has one now (GPlatformFilterRows)
    boolean drawsRows() {
        return hasFiltersContainer;
    }
    boolean hasRows() {
        return !boxRows.isEmpty();
    }

    // ... and, for the panel's input (GPlatformFilterInputController), whether a view's batch runs: a focus waits for
    // it
    boolean isBatching() {
        return form.isBatching();
    }
}
