package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GInputEvent;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.view.GFilterView;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;

public abstract class GUserFilters {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String FILTER_ICON_PATH = "filt.png";

    private GToolbarButton toolbarButton;

    private ArrayList<GPropertyFilter> conditions = new ArrayList<>();

    private GTableController logicsSupplier;

    public GFilterView filterView;

    public GUserFilters(GTableController logicsSupplier, GFilter filter) {
        this.logicsSupplier = logicsSupplier;

        filterView = new GFilterView(this, filter);

        toolbarButton = new GToolbarButton(FILTER_ICON_PATH) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    filterView.toggleToolsVisible();
                    updateToolbarButton();
                };
            }
        };
        updateToolbarButton();
    }

    public Button getToolbarButton() {
        return toolbarButton;
    }

    public boolean hasConditions() {
        return filterView.hasConditions();
    }

    public GTableController getLogicsSupplier() {
        return logicsSupplier;
    }

    public Widget getView() {
        return filterView;
    }

    private void updateToolbarButton() {
        toolbarButton.setTitle(filterView.isToolsVisible() ? messages.hideUserFilterTools() : messages.showUserFilterTools());
        toolbarButton.showBackground(filterView.isToolsVisible());
    }

    public void addConditionPressed(boolean replace, Event event) {
        filterView.addCondition(event, replace);
    }

    public GPropertyFilter getNewCondition(GPropertyDraw property, GGroupObjectValue columnKey) {
        GPropertyDraw filterProperty = property;
        GGroupObjectValue filterColumnKey = columnKey;
        Object filterValue = null;

        if (filterProperty == null) {
            filterProperty = logicsSupplier.getSelectedProperty();
            if (filterProperty != null) {
                filterColumnKey = logicsSupplier.getSelectedColumnKey();
                filterValue = logicsSupplier.getSelectedValue(filterProperty, filterColumnKey);
            }
        }
        if (filterProperty == null)
            return null;

        return new GPropertyFilter(logicsSupplier.getSelectedGroupObject(), filterProperty, filterColumnKey, filterValue, filterProperty.getDefaultCompare());
    }

    public void applyFilters(ArrayList<GPropertyFilter> filters, boolean replace) {
        if (replace) {
            conditions = filters;
        } else {
            for (GPropertyFilter filter : filters) {
                if (!hasCondition(filter)) {
                    conditions.add(filter);
                }
            }
        }
        applyFilters(conditions);
    }

    private boolean hasCondition(GPropertyFilter newCondition) {
        assert newCondition.junction;
        for(int i = 0; i < conditions.size(); i++) {
            GPropertyFilter prevCondition = i > 0 ? conditions.get(i - 1) : null;
            GPropertyFilter condition = conditions.get(i);
            if((prevCondition == null || prevCondition.junction) &&
                    condition.junction &&
                    condition.negation == newCondition.negation &&
                    condition.groupObject.equals(newCondition.groupObject) &&
                    condition.property.equals(newCondition.property) &&
                    condition.value.equals(newCondition.value) &&
                    condition.columnKey.equals(newCondition.columnKey) &&
                    condition.compare == newCondition.compare) {
                return true;
            }
        }
        return false;
    }

    public void allRemovedPressed() {
        filterView.allRemovedPressed();
    }

    public void allRemoved() {
        conditions.clear();
        applyFilters(conditions);
    }

    public void quickEditFilter(Event keyEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        filterView.addCondition(propertyDraw, columnKey, keyEvent, true);
    }

    public abstract void applyFilters(ArrayList<GPropertyFilter> conditions);
    public abstract void checkCommitEditing();
    public abstract void addBinding(GInputEvent event, GBindingEnv env, GFormController.BindingExec pressed, Widget component);

    public void setVisible(boolean visible) {
        getView().setVisible(visible);
    }

    public void update() {
        filterView.update();
    }
}
