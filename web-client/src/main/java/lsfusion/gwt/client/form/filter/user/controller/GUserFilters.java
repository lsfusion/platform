package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.view.GFilterView;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class GUserFilters {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String FILTER = "filt.png";

    private GToolbarButton toolbarButton;

    private List<GPropertyFilter> conditions = new ArrayList<>();

    private GTableController logicsSupplier;

    public GUserFilters(GTableController logicsSupplier) {
        this.logicsSupplier = logicsSupplier;

        toolbarButton = new GToolbarButton(FILTER) {
            @Override
            public void addListener() {
                addClickHandler(event -> showDialog(null, null, null, false, false));
            }
        };
        updateToolbarButton();
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

    public GTableController getLogicsSupplier() {
        return logicsSupplier;
    }

    private void showDialog(Event keyEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey, boolean replace, boolean alwaysAddNew) {
        List<GPropertyFilter> conditions = replace ? new ArrayList<>() : new ArrayList<>(this.conditions);
        if(alwaysAddNew || conditions.isEmpty()) {
            conditions.add(getNewCondition(propertyDraw, columnKey));
        }
        new GFilterView(this).showDialog(conditions, logicsSupplier, keyEvent, propertyDraw);
    }

    private void updateToolbarButton() {
        boolean hasConditions = hasConditions();
        toolbarButton.setTitle(hasConditions ? messages.expandFilterWindow() : (messages.formQueriesFilter() + " (F2)"));
        toolbarButton.showBackground(hasConditions);
    }

    public void addConditionPressed(boolean replace) {
        showDialog(null, null, null, replace, true);
    }

    public GPropertyFilter getNewCondition(GPropertyDraw property, GGroupObjectValue columnKey) {
        GPropertyDraw filterProperty = property;
        GGroupObjectValue filterColumnKey = columnKey;

        if(filterProperty == null) {
            filterProperty = logicsSupplier.getSelectedProperty();
            if(filterProperty != null)
                filterColumnKey = logicsSupplier.getSelectedColumn();
        }
        if (filterProperty == null) {
            //не добавляем, если нет ни одного свойства
            return null;
        }

        GPropertyFilter filter = new GPropertyFilter();
        filter.property = filterProperty;
        filter.columnKey = filterColumnKey;
        GDataFilterValue filterValue = new GDataFilterValue();
        filterValue.value = (Serializable) logicsSupplier.getSelectedValue(filterProperty, filterColumnKey);
        filter.value = filterValue;
        filter.groupObject = logicsSupplier.getSelectedGroupObject();
        filter.compare = filter.getDefaultCompare();
        return filter;
    }

    public void applyFilters(List<GPropertyFilter> filters, boolean replace) {
        if(replace) {
            conditions = filters;
        } else {
            for (GPropertyFilter filter : filters) {
                GPropertyFilter condition = findConditionByProperty(filter.property);
                if (condition != null) {
                    conditions.remove(condition);
                }
                conditions.add(filter);
            }
        }
        applyQuery();
    }

    private GPropertyFilter findConditionByProperty(GPropertyDraw property) {
        for(GPropertyFilter condition : conditions) {
            if(condition.property.equals(property)) {
                return condition;
            }
        }
        return null;
    }

    public void allRemovedPressed() {
        conditions.clear();
        applyQuery();
    }

    public void applyQuery() {
        remoteApplyQuery();
        updateToolbarButton();
    }

    public void quickEditFilter(Event keyEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        showDialog(keyEvent, propertyDraw, columnKey, true, true);
    }

    public abstract void remoteApplyQuery();
    public abstract void filterHidden();
    public abstract void checkCommitEditing();
}
