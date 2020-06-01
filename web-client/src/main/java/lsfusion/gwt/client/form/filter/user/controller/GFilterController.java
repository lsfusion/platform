package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.user.client.ui.Button;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.view.GFilterView;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class GFilterController {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String FILTER = "filt.png";

    private GToolbarButton toolbarButton;
    private GFilterView filterView;

    private List<GPropertyFilter> conditions = new ArrayList<>();

    private GTableController logicsSupplier;

    public GFilterController(GTableController logicsSupplier) {
        this.logicsSupplier = logicsSupplier;

        toolbarButton = new GToolbarButton(FILTER) {
            @Override
            public void addListener() {
                addClickHandler(event -> showFilterPressed());
            }
        };
        updateToolbarButton();

        filterView = new GFilterView(this);
    }

    public Button getToolbarButton() {
        return toolbarButton;
    }

    public List<GPropertyFilter> getConditions() {
        return conditions;
    }

    public boolean hasConditions() {
        return !conditions.isEmpty();
    }

    private void showFilterPressed() {
        if (hasConditions() || addNewCondition(false)) {
            showDialog();
        }
    }

    private void showDialog() {
        filterView.showDialog(conditions, logicsSupplier);
    }

    private void hideDialog() {
        updateToolbarButton();
        filterHidden();
        filterView.hideDialog();
    }

    private void updateToolbarButton() {
        boolean hasConditions = hasConditions();
        toolbarButton.setTitle(hasConditions ? messages.expandFilterWindow() : (messages.formQueriesFilter() + " (F2)"));
        toolbarButton.showBackground(hasConditions);
    }

    public void addConditionPressed(boolean replace) {
        if (addNewCondition(replace)) {
            showDialog();
        }
    }

    public boolean addNewCondition(boolean replace) {
        return addNewCondition(replace, null, null);
    }

    private boolean addNewCondition(boolean replace, GPropertyDraw property, GGroupObjectValue columnKey) {
        GPropertyDraw filterProperty = property;
        GGroupObjectValue filterColumnKey = columnKey;

        if(filterProperty == null) {
            filterProperty = logicsSupplier.getSelectedProperty();
            if(filterProperty != null)
                filterColumnKey = logicsSupplier.getSelectedColumn();
        }
        if (filterProperty == null) {
            //не добавляем, если нет ни одного свойства
            return false;
        }

        if (replace) {
            removeAllConditions();
        }

        GPropertyFilter filter = new GPropertyFilter();
        filter.property = filterProperty;
        filter.columnKey = filterColumnKey;
        filter.groupObject = logicsSupplier.getSelectedGroupObject();
        filterView.addCondition(filter, logicsSupplier, false);
        return true;
    }

    public void applyFilters(List<GPropertyFilter> filters) {
        for(GPropertyFilter filter : filters) {
            GPropertyFilter condition = findConditionByProperty(filter.property);
            if(condition != null) {
                conditions.remove(condition);
            }
            conditions.add(filter);
        }
        applyQuery();
        updateToolbarButton();
    }

    private GPropertyFilter findConditionByProperty(GPropertyDraw property) {
        for(GPropertyFilter condition : conditions) {
            if(condition.property.equals(property)) {
                return condition;
            }
        }
        return null;
    }

    private void removeAllConditions() {
        conditions.clear();
        filterView.removeAllConditions();
    }

    public void allRemovedPressed() {
        removeAllConditions();
        applyQuery();
        hideDialog();
    }

    public void cancelFilter() {
        filterView.removeAllConditions();
        hideDialog();
    }

    public void applyFilter(List<GPropertyFilter> conditions) {
        this.conditions = conditions;
        filterView.removeAllConditions();
        applyQuery();
        hideDialog();
    }

    private void applyQuery() {
        remoteApplyQuery();
    }

    public void quickEditFilter(EditEvent keyEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        if (addNewCondition(true, propertyDraw, columnKey)) {
            showDialog();
            filterView.startEditing(keyEvent, propertyDraw);
        }
    }

    public abstract void remoteApplyQuery();
    public abstract void filterHidden();
}
