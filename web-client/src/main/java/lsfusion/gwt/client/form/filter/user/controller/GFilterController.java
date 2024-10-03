package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.filter.user.view.GFilterControlsView;
import lsfusion.gwt.client.form.filter.user.view.GFiltersHandler;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public abstract class GFilterController implements GFilterConditionView.UIHandler, GFiltersHandler {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private GTableController logicsSupplier;
    private Map<Column, String> columns = new HashMap<>();
    
    private GToolbarButton toolbarButton;

    private boolean hasFiltersContainer;

    private Map<GPropertyFilter, GFilterConditionView> conditionViews = new LinkedHashMap<>();

    private List<GFilter> initialFilters;
    
    private boolean controlsVisible = false;
    private GFilterControlsView controlsView;

    public GFilterController(GTableController logicsSupplier, List<GFilter> filters, GFilterControls filterControls, boolean hasFiltersContainer) {
        this.logicsSupplier = logicsSupplier;
        this.initialFilters = filters;
        this.hasFiltersContainer = hasFiltersContainer;

        toolbarButton = new GToolbarButton(StaticImage.FILTER) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    toggleControlsVisible();
                };
            }
        };
        updateToolbarButton();

        controlsView = new GFilterControlsView(this);
        controlsView.setVisible(controlsVisible);
        logicsSupplier.getForm().formLayout.addBaseComponent(filterControls, controlsView, null);
    }

    public GToolbarButton getToolbarButton() {
        return toolbarButton;
    }

    private void processBinding(NativeEvent event, Runnable action) {
        action.run();
        stopPropagation(event);
    }

    @Override
    public void addEnterBinding(Widget widget) {
        addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER)),
                new GBindingEnv(null, GBindingMode.ALL, null, GBindingMode.ONLY, null, null, GBindingMode.ONLY, null),
                event -> processBinding(event, () -> applyFilters()),
                widget);
    }

    public void toggleControlsVisible() {
        setControlsVisible(!controlsVisible);

        if (controlsVisible && !hasNotFixedConditions() && hasFiltersContainer()) {
            addCondition();
        }
    }

    private boolean hasNotFixedConditions() {
        for(GPropertyFilter condition : conditionViews.keySet()) {
            if(!condition.isFixed()) {
                return true;
            }
        }
        return false;
    }
    
    private void hideControlsIfEmpty() {
        if (!hasConditions()) {
            setControlsVisible(false);
        }
    }
    
    public void setControlsVisible(boolean visible) {
        controlsVisible = visible;

        for (GFilterConditionView view : conditionViews.values()) {
            view.setControlsVisible(controlsVisible);
        }

        controlsView.setVisible(controlsVisible);
        logicsSupplier.getForm().formLayout.update(-1);

        updateToolbarButton();
    }
    
    private void updateToolbarButton() {
        toolbarButton.setTitle(controlsVisible ? messages.formFilterHideControls() : messages.formFilterShowControls());
        toolbarButton.showBackground(controlsVisible);
    }
    
    public GContainer getFiltersContainer() {
        return logicsSupplier.getFiltersContainer();
    }

    public static GPropertyFilter createNewCondition(GTableController logicsSupplier, GFilter filter, GGroupObjectValue columnKey) {
        return createNewCondition(logicsSupplier, filter, columnKey, null, null, null, null);
    }

    public static GPropertyFilter createNewCondition(GTableController logicsSupplier, GFilter filter, GGroupObjectValue columnKey, PValue value, Boolean negation, GCompare compare, Boolean junction) {
        Pair<GPropertyDraw, GGroupObjectValue> column = getActualColumn(logicsSupplier, filter != null ? filter.property: null, columnKey);
        
        if (column.first == null)
            return null;

        if (filter == null) {
            filter = new GFilter(column.first);
        } else if (filter.property == null) {
            filter.property = column.first;
        }

        return new GPropertyFilter(filter, logicsSupplier.getSelectedGroupObject(), column.second, value, negation, compare, junction);
    }

    private static Pair<GPropertyDraw, GGroupObjectValue> getActualColumn(GTableController logicsSupplier, GPropertyDraw property, GGroupObjectValue columnKey) {
        GPropertyDraw actualProperty = property;
        GGroupObjectValue actualColumnKey = columnKey;

        if (actualProperty == null) {
            actualProperty = logicsSupplier.getSelectedFilterProperty();
            if (actualProperty != null) {
                actualColumnKey = logicsSupplier.getSelectedColumnKey();
            }
        }
        return new Pair<>(actualProperty, actualColumnKey);
    }

    public void addCondition() {
        // pass add filter key down event to start editing immediately
        addCondition(GKeyStroke.createAddUserFilterKeyEvent());
    }

    public void addCondition(Event keyEvent) {
        addCondition(keyEvent, false);
    }

    public void addCondition(Event keyEvent, boolean replace) {
        addCondition(keyEvent, replace, false);
    }

    public void addCondition(Event keyEvent, boolean replace, boolean checkExistingFilter) {
        addCondition(null, null, keyEvent, replace, true, checkExistingFilter);
    }

    public void addCondition(GFilter filter, GGroupObjectValue columnKey, boolean readSelectedValue) {
        addCondition(filter, columnKey, null, false, readSelectedValue);
    }

    public void addCondition(GPropertyDraw propertyDraw, GGroupObjectValue columnKey, Event keyEvent, boolean replace, boolean readSelectedValue, boolean checkExistingFilter) {
        if (checkExistingFilter) {
            GFilterConditionView existingFilter = findExistingFilter(propertyDraw, columnKey);
            if (existingFilter != null) {
                if (readSelectedValue) {
                    existingFilter.putSelectedValue();
                }
                existingFilter.focusValueView(); // to leave focus on value with manual apply mode and be able to apply on Enter
                existingFilter.startEditing(keyEvent);
                return;
            }
        }
        addCondition(new GFilter(propertyDraw), columnKey, keyEvent, replace, readSelectedValue);
    }

    public void addCondition(GFilter filter, GGroupObjectValue columnKey, Event keyEvent, boolean replace, boolean readSelectedValue) {
        addCondition(createNewCondition(logicsSupplier, filter, columnKey), keyEvent, replace, readSelectedValue);
    }

    public void addCondition(GPropertyFilter condition, Event keyEvent, boolean replace, boolean readSelectedValue) {
        this.addCondition(condition, keyEvent, replace, readSelectedValue, true);
    }

    public void addCondition(GPropertyFilter condition, Event keyEvent, boolean replace, boolean readSelectedValue, boolean focusOnValue) {
        if (replace) {
            resetAllConditions(false);
        }
        if (condition != null) {
            if (condition.filter.container == null)
                getFiltersContainer().add(condition.filter);

            GFilterConditionView conditionView = new GFilterConditionView(condition, logicsSupplier, this, () -> columns, controlsVisible, readSelectedValue);

            logicsSupplier.getForm().getFormLayout().addBaseComponent(condition.filter, conditionView.initView(), null);
            conditionViews.put(condition, conditionView);

            updateConditionsLastState();

            logicsSupplier.getForm().getFormLayout().update(-1);

            conditionView.onAdd(focusOnValue);

            if (keyEvent != null) {
                conditionView.startEditing(keyEvent);
            }
        }
    }

    public void addConditions(ArrayList<GPropertyFilter> conditions, boolean focusFirstComponent, boolean replace) {
        if (replace) {
            removeAllConditionsWithoutApply();
        }
        
        for (GPropertyFilter condition : conditions) {
            addCondition(condition, null, false, false);
        }
        
        applyFilters(focusFirstComponent, null);
    }
    
    private GFilterConditionView findExistingFilter(GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        Pair<GPropertyDraw, GGroupObjectValue> column = getActualColumn(logicsSupplier, propertyDraw, columnKey);

        for (GPropertyFilter filter : conditionViews.keySet()) {
            if (filter.columnEquals(column)) {
                return conditionViews.get(filter);
            }
        }
        
        return null;
    }

    public void changeFilters(List<GPropertyFilter> filters) {
        if (hasFiltersContainer()) {
            // hide controls only if no filters are expected. otherwise leave controls visibility unchanged
            removeAllConditionsWithoutApply(filters.isEmpty());

            Set<GPropertyFilter> fixedFilters = new LinkedHashSet<>(conditionViews.keySet());

            for (GPropertyFilter filter : filters) {
                boolean filterExists = false;
                for (GPropertyFilter fixedFilter : fixedFilters) {
                    if (filter.columnEquals(fixedFilter)) {
                        fixedFilter.override(filter);
                        conditionViews.get(fixedFilter).applyCondition(filter);
                        filterExists = true;
                        fixedFilters.remove(fixedFilter);
                        break;
                    }
                }

                if (!filterExists) {
                    addCondition(filter, null, false, false, false);
                }
            }

            // the only changeFilters() call is made when filters are initiated by server via FilterClientAction
            // in this case we don't want focus to appear on some unexpected grid
            applyFilters(false, null);
        }
    }

    private void removeConditionView(GPropertyFilter condition) {
        GFormLayout layout = logicsSupplier.getForm().getFormLayout();
        layout.removeBaseComponent(condition.filter);
        getFiltersContainer().removeFromChildren(condition.filter);
    }

    @Override
    public void removeCondition(GPropertyFilter condition) {
        removeConditionViewInner(condition);

        hideControlsIfEmpty();
        
        updateConditionsLastState();

        applyFilters(true, null);
    }

    public ArrayList<GFilterConditionView> removeAllConditionsWithoutApply() {
        return removeAllConditionsWithoutApply(true);
    }
    
    public ArrayList<GFilterConditionView> removeAllConditionsWithoutApply(boolean hideControls) {
        ArrayList<GFilterConditionView> changed = new ArrayList<>();
        for (GPropertyFilter filter : new LinkedHashMap<>(conditionViews).keySet()) {
            if (filter.isFixed()) {
                GFilterConditionView filterView = conditionViews.get(filter);
                if(filterView.clearValueView())
                    changed.add(filterView);
            } else {
                removeConditionViewInner(filter);
            }
        }
        if (hideControls) {
            hideControlsIfEmpty();
        }
        return changed;
    }

    @Override
    public void resetConditions() {
        resetAllConditions(true);
    }

    @Override
    public boolean isManualApplyMode() {
        return MainFrame.userFiltersManualApplyMode && controlsVisible;
    }

    public void resetAllConditions(boolean focusFirstComponent) {
        ArrayList<GFilterConditionView> changed = removeAllConditionsWithoutApply();
        
        applyFilters(new ArrayList<>(), changed, focusFirstComponent);
    }

    private void removeConditionViewInner(GPropertyFilter filter) {
        removeConditionView(filter);
        GFilterConditionView filterView = conditionViews.remove(filter);
        filterView.isRemoved = true;
    }

    private void updateConditionsLastState() {
        int i = 0;
        for (GFilterConditionView cView : conditionViews.values()) {
            i++;
            cView.setLast(i == conditionViews.size());
        }
    }

    public void applyFilters() {
        applyFilters(true, null);
    }


    public void applyFilters(boolean focusFirstComponent, GFilterConditionView changedView) {
        ArrayList<GPropertyFilter> result = new ArrayList<>();
        for (Map.Entry<GPropertyFilter, GFilterConditionView> entry : conditionViews.entrySet()) {
            GFilterConditionView conditionView = entry.getValue();
            if (!entry.getKey().nullValue() || conditionView.allowNull) {
                result.add(entry.getKey());
                conditionView.setApplied(true);
            } else {
                conditionView.setApplied(false);
            }
        }
        ArrayList<GFilterConditionView> changed = new ArrayList<>();
        if(changedView != null)
            changed.add(changedView);
        applyFilters(result, changed, focusFirstComponent);

        controlsView.setApplyEnabled(false);
    }

    @Override
    public void enableApplyButton() {
        controlsView.setApplyEnabled(true);
    }

    public void update() {
        columns.clear();
        for (Pair<Column, String> column : logicsSupplier.getFilterColumns()) {
            columns.put(column.first, column.second);
        }
        
        if (initialFilters != null) {
            for (GFilter filter : initialFilters) {
                if (filter.container != null) { // removed in design
                    filter.fixed = true;
                    addCondition(filter, logicsSupplier.getSelectedColumnKey(), false);
                }
            }
            initialFilters = null;
        }
    }

    public void quickEditFilter(Event keyEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        addCondition(propertyDraw, columnKey, keyEvent, false, true, true);
    }

    public boolean hasFiltersContainer() {
        return hasFiltersContainer;
    }

    public void setVisible(boolean visible) {
        for(GFilterConditionView conditionView : conditionViews.values())
            GwtClientUtils.setGridVisible(conditionView, visible);
        GwtClientUtils.setGridVisible(controlsView, visible && controlsVisible);
    }
    
    public boolean hasConditions() {
        return !conditionViews.isEmpty();
    }
    
    public boolean hasConditionsToReset() {
        for (GFilterConditionView condition : conditionViews.values()) {
            if (!condition.isFixed() || condition.isApplied()) {
                return true;
            }
        }
        return false;
    }
    
    public abstract void applyFilters(ArrayList<GPropertyFilter> conditions, ArrayList<GFilterConditionView> changed, boolean focusFirstComponent);
    public abstract void addBinding(GInputEvent event, GBindingEnv env, GFormController.BindingExec pressed, Widget component);
}
