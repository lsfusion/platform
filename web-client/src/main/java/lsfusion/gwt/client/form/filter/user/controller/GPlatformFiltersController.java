package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.filter.user.view.GFilterEditors;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.controller.GUserFiltersController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

// the platform's FILTERS of a group, or of a tree for all its groups: its filter panel, both what shows the user
// filters (GUserFiltersController) and what takes their input (GFilterInputController) - the conditions it draws in the
// FILTERS box show the list as the server reports it, and the input works on them: added with an editor, applied as its
// rows, reset. Its editors of them are what the controls and the toolbar's button show and apply (GFilterEditors)
public final class GPlatformFiltersController implements GFilterInputController, GUserFiltersController, GFilterConditionView.UIHandler, GFilterEditors {
    private final GFormController form;
    private final GTableController rows;
    private final GContainer filtersContainer;
    private Map<Column, String> columns = new HashMap<>();
    
    private boolean hasFiltersContainer;

    private Map<GPropertyFilter, GFilterConditionView> conditionViews = new LinkedHashMap<>();

    private List<GFilter> initialFilters;

    // the controls and the toolbar's button, which show and hide the conditions' editors with them
    private final GFilterControlsToggle toggle;
    
    public GPlatformFiltersController(GFormController form, GTableController rows, GContainer filtersContainer, List<GFilter> filters, GFilterControlsToggle toggle) {
        this.form = form;
        this.rows = rows;
        this.filtersContainer = filtersContainer;
        this.initialFilters = filters;
        this.hasFiltersContainer = form.formLayout.getContainerView(filtersContainer) != null;

        this.toggle = toggle;
        toggle.bind(this, this); // its commands, and its editors
    }

    public boolean isControlsVisible() {
        return toggle.isVisible();
    }
    public void setControlsVisible(boolean visible) {
        toggle.setVisible(visible);
    }

    private void processBinding(NativeEvent event, Runnable action) {
        action.run();
        stopPropagation(event);
    }

    @Override
    public void addEnterBinding(Widget widget) {
        form.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER)),
                new GBindingEnv(null, GBindingMode.ALL, null, null, GBindingMode.ONLY, null, null, GBindingMode.ONLY, null),
                event -> processBinding(event, () -> applyFilters()),
                widget, rows.getSelectedGroupObject());
    }

    @Override
    public void toggleControlsVisible() {
        setControlsVisible(!toggle.isVisible());

        if (toggle.isVisible() && !hasNotFixedConditions() && hasFiltersContainer()) {
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
    
    // ... and the conditions' editors with them: their selectors and their delete buttons
    public void setConditionsControlsVisible(boolean visible) {
        for (GFilterConditionView view : conditionViews.values()) {
            view.setControlsVisible(visible);
        }
    }
    
    private static GPropertyFilter createNewCondition(GTableController rows, GFilter filter, GGroupObjectValue columnKey) {
        Column column = getActualColumn(rows, filter != null ? filter.property: null, columnKey);
        
        if (column.property == null)
            return null;

        if (filter == null) {
            filter = new GFilter(column.property);
        } else if (filter.property == null) {
            filter.property = column.property;
        }

        // a condition is of the group of its property, as the server takes it, a tree's selected group or not
        return new GPropertyFilter(filter, column.property.groupObject, column.columnKey, null, null);
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
        addCondition(createNewCondition(rows, filter, columnKey), keyEvent, replace, readSelectedValue);
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
                filtersContainer.add(condition.filter);

            GFilterConditionView conditionView = new GFilterConditionView(condition, form, rows, this, () -> columns, toggle.isVisible(), readSelectedValue);

            form.getFormLayout().addBaseComponent(condition.filter, conditionView.initView(), null);
            conditionViews.put(condition, conditionView);

            updateConditionsLastState();

            form.getFormLayout().update(-1);

            if (keyEvent != null)
                enableApplyButton();

            // its value takes the focus, and the key starts its editor, which takes it too - once the batch is sent,
            // where a view's member added it in one: a focus sends a request of the platform's own, a property made
            // active, which a batch does not carry
            Scheduler.ScheduledCommand focus = () -> {
                conditionView.onAdd(focusOnValue);
                if (keyEvent != null)
                    conditionView.startEditing(keyEvent);
            };
            if (form.isBatching())
                Scheduler.get().scheduleDeferred(focus);
            else
                focus.execute();
        }
    }

    public void replaceConditions(ArrayList<GPropertyFilter> conditions) {
        removeAllConditionsWithoutApply();

        for (GPropertyFilter condition : conditions) {
            addCondition(condition, null, false, false);
        }
        
        applyFilters(false, null);
    }
    
    private GFilterConditionView findExistingFilter(GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        Column column = getActualColumn(rows, propertyDraw, columnKey);

        for (GPropertyFilter filter : conditionViews.keySet()) {
            if (filter.column.equals(column)) {
                return conditionViews.get(filter);
            }
        }
        
        return null;
    }

    // the filters the server reports for a group, which it has applied already, take the place of the group's
    // conditions; nothing is sent. A fixed condition allows NULL only where the server has it with no value
    @Override
    public void updateFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filterDTOs) {
        if (hasFiltersContainer()) {
            ArrayList<GPropertyFilter> filters = GPropertyFilter.getConditions(form.getForm(), group, filterDTOs);
            Set<GPropertyFilter> fixedFilters = new LinkedHashSet<>();
            for (GPropertyFilter condition : new ArrayList<>(conditionViews.keySet())) {
                if (condition.groupObject.equals(group)) {
                    if (condition.isFixed()) {
                        GFilterConditionView conditionView = conditionViews.get(condition);
                        conditionView.clearValueView();
                        conditionView.setAllowNull(false);
                        fixedFilters.add(condition);
                    } else {
                        removeConditionViewInner(condition);
                    }
                }
            }

            for (GPropertyFilter filter : filters) {
                boolean filterExists = false;
                for (GPropertyFilter fixedFilter : fixedFilters) {
                    if (filter.column.equals(fixedFilter.column)) {
                        fixedFilter.override(filter);
                        GFilterConditionView conditionView = conditionViews.get(fixedFilter);
                        conditionView.applyCondition(filter);
                        conditionView.setAllowNull(filter.nullValue());
                        filterExists = true;
                        fixedFilters.remove(fixedFilter);
                        break;
                    }
                }

                if (!filterExists) {
                    addCondition(filter, null, false, false, false);
                }
            }

            // hide controls only if no filters are expected. otherwise leave controls visibility unchanged
            if (filters.isEmpty()) {
                hideControlsIfEmpty();
            }
            updateConditionsLastState();

            // the Apply button is left as it is: the conditions of the tree's other groups may wait for it
            for (Map.Entry<GPropertyFilter, GFilterConditionView> entry : conditionViews.entrySet()) {
                if (entry.getKey().groupObject.equals(group)) {
                    entry.getValue().setApplied(isApplied(entry.getKey(), entry.getValue()));
                }
            }
        }
    }

    // ... and the ones the form has just sent, which it tells what shows the user filters of each of the box's groups,
    // of whichever kind it holds, before the server answers (GFormController.applyCurrentFilters): a view's part shows
    // them then; this panel, the one that states a list where it exists, shows them already
    @Override
    public void changeFilters(GGroupObject group, ArrayList<GPropertyFilterDTO> filters) {
    }

    private void removeConditionView(GPropertyFilter condition) {
        GFormLayout layout = form.getFormLayout();
        layout.removeBaseComponent(condition.filter);
        filtersContainer.removeFromChildren(condition.filter);
    }

    @Override
    public void removeCondition(GPropertyFilter condition) {
        removeConditionViewInner(condition);

        hideControlsIfEmpty();
        
        updateConditionsLastState();

        applyFilters(true, null);
    }

    public ArrayList<GFilterConditionView> removeAllConditionsWithoutApply() {
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
        hideControlsIfEmpty();
        return changed;
    }

    @Override
    public void resetConditions() {
        resetAllConditions(true);
    }

    @Override
    public boolean isManualApplyMode() {
        return MainFrame.userFiltersManualApplyMode && toggle.isVisible();
    }

    public void resetAllConditions(boolean focusFirstComponent) {
        ArrayList<GFilterConditionView> changed = removeAllConditionsWithoutApply();
        
        applyFilters(new ArrayList<>(), changed, focusFirstComponent);
    }

    private void removeConditionViewInner(GPropertyFilter filter) {
        removeConditionView(filter);
        GFilterConditionView filterView = conditionViews.remove(filter);
        filterView.isRemoved = true;
        filterView.destroy();
    }

    // the form is closing: a condition still on screen holds its value cell, and that cell is registered for the life
    // of the page
    public void destroy() {
        for(GFilterConditionView conditionView : conditionViews.values())
            conditionView.destroy();
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


    // a condition with no value is applied only where it allows NULL
    private static boolean isApplied(GPropertyFilter condition, GFilterConditionView conditionView) {
        return !condition.nullValue() || conditionView.allowNull;
    }

    public void applyFilters(boolean focusFirstComponent, GFilterConditionView changedView) {
        ArrayList<GPropertyFilter> result = new ArrayList<>();
        for (Map.Entry<GPropertyFilter, GFilterConditionView> entry : conditionViews.entrySet()) {
            GFilterConditionView conditionView = entry.getValue();
            if (isApplied(entry.getKey(), conditionView)) {
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

        toggle.setApplyEnabled(false);
    }

    @Override
    public void enableApplyButton() {
        toggle.setApplyEnabled(true);
    }

    // what draws the rows has changed, once per delta (GFilterInputController.update): the columns a condition can be
    // on, read anew, and the design's conditions, added the first time
    public void update() {
        columns.clear();
        for (Pair<Column, String> column : rows.getFilterColumns()) {
            columns.put(column.first, column.second);
        }
        
        if (initialFilters != null) {
            for (GFilter filter : initialFilters) {
                if (filter.container != null) { // removed in design
                    filter.fixed = true;
                    addCondition(filter, rows.getSelectedColumnKey(), false);
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

    @Override
    public void setVisible(boolean visible) {
        for(GFilterConditionView conditionView : conditionViews.values())
            GwtClientUtils.setGridVisible(conditionView, visible);
        toggle.setTableVisible(visible);
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
    
    // the conditions stated as the group's list, each changed one marked loading until the answer
    private void applyFilters(ArrayList<GPropertyFilter> conditions, ArrayList<GFilterConditionView> changed, boolean focusFirstComponent) {
        long requestIndex = rows.changeFilter(conditions);

        for (GFilterConditionView filterView : changed)
            form.setLoading(filterView, requestIndex);

        if (focusFirstComponent)
            Scheduler.get().scheduleDeferred(() -> rows.focusFirstWidget(FocusUtils.Reason.APPLYFILTER));
    }
}
