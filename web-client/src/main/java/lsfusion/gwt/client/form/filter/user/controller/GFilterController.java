package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.view.Column;

import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public abstract class GFilterController implements GFilterConditionView.UIHandler {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String ADD_ICON_PATH = "filtadd.png";
    private static final String RESET_ICON_PATH = "filtreset.png";
    private static final String FILTER_ICON_PATH = "filt.png";

    private GTableController logicsSupplier;
    private Map<Column, String> columns = new HashMap<>();
    
    private GToolbarButton toolbarButton;
    private GToolbarButton addConditionButton;
    private GToolbarButton resetConditionsButton;

    private boolean hasFiltersContainer;

    private Map<GPropertyFilter, GFilterConditionView> conditionViews = new LinkedHashMap<>();

    private List<GFilter> initialFilters;
    
    private boolean toolsVisible = false;

    public GFilterController(GTableController logicsSupplier, List<GFilter> filters, boolean hasFiltersContainer) {
        this.logicsSupplier = logicsSupplier;
        this.initialFilters = filters;
        this.hasFiltersContainer = hasFiltersContainer;

        toolbarButton = new GToolbarButton(FILTER_ICON_PATH) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    toggleToolsVisible();
                    updateToolbarButton();
                };
            }
        };
        updateToolbarButton();

        if (hasFiltersContainer()) {
            addConditionButton = new GToolbarButton(ADD_ICON_PATH, messages.formFilterAddCondition()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> addCondition();
                }
            };
            addConditionButton.addStyleName("userFilterButton");
            addConditionButton.setVisible(false);
        }

        resetConditionsButton = new GToolbarButton(RESET_ICON_PATH, messages.formFilterResetConditions()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    resetAllConditions();
                    toggleToolsVisible();
                    updateToolbarButton();
                };
            }
        };
        resetConditionsButton.addStyleName("userFilterButton");
        resetConditionsButton.setVisible(false);
    }

    public Button getToolbarButton() {
        return toolbarButton;
    }

    public GToolbarButton getAddFilterConditionButton() {
        return addConditionButton;
    }

    public GToolbarButton getResetFiltersButton() {
        return resetConditionsButton;
    }

    // similar to GFormController.processBinding
    private void processBinding(NativeEvent event, Runnable action) {
        logicsSupplier.getForm().checkCommitEditing();
        action.run();
        stopPropagation(event);
    }

    @Override
    public void addEnterBinding(Widget widget) {
        addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER)),
                new GBindingEnv(null, null, null, null, null, null, GBindingMode.ONLY, null),
                event -> processBinding(event, () -> applyFilters(true)),
                widget);
    }

    public void toggleToolsVisible() {
        toolsVisible = !toolsVisible;

        if (!conditionViews.isEmpty()) {
            for (GFilterConditionView view : conditionViews.values()) {
                view.setToolsVisible(toolsVisible);
            }
        } else if (toolsVisible && hasFiltersContainer()) {
            addCondition();
        }

        if (addConditionButton != null) {
            addConditionButton.setVisible(toolsVisible);
        }
        resetConditionsButton.setVisible(toolsVisible);
    }
    private void updateToolbarButton() {
        toolbarButton.setTitle(toolsVisible ? messages.formFilterHideTools() : messages.formFilterShowTools());
        toolbarButton.showBackground(toolsVisible);
    }
    
    public GContainer getFiltersContainer() {
        return logicsSupplier.getFiltersContainer();
    }

    public GPropertyFilter getNewCondition(GFilter filter, GGroupObjectValue columnKey) {
        Pair<GPropertyDraw, GGroupObjectValue> column = getActualColumn(filter != null ? filter.property: null, columnKey);
        
        if (column.first == null)
            return null;

        if (filter == null) {
            filter = new GFilter(column.first);
        } else if (filter.property == null) {
            filter.property = column.first;
        }

        return new GPropertyFilter(filter, logicsSupplier.getSelectedGroupObject(), column.second, null, column.first.getDefaultCompare());
    }

    private Pair<GPropertyDraw, GGroupObjectValue> getActualColumn(GPropertyDraw property, GGroupObjectValue columnKey) {
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
        addCondition((GFilter) null, null, keyEvent, replace, true);
    }

    public void addCondition(GFilter filter, GGroupObjectValue columnKey, boolean readSelectedValue) {
        addCondition(filter, columnKey, null, false, readSelectedValue);
    }

    public void addCondition(GPropertyDraw propertyDraw, GGroupObjectValue columnKey, Event keyEvent, boolean replace, boolean readSelectedValue) {
        addCondition(new GFilter(propertyDraw), columnKey, keyEvent, replace, readSelectedValue);
    }

    public void addCondition(GFilter filter, GGroupObjectValue columnKey, Event keyEvent, boolean replace, boolean readSelectedValue) {
        addCondition(getNewCondition(filter, columnKey), keyEvent, replace, readSelectedValue);
    }

    public void addCondition(GPropertyFilter condition, Event keyEvent, boolean replace, boolean readSelectedValue) {
        if (replace) {
            resetAllConditions(false);
        }
        if (condition != null) {
            GFilterConditionView conditionView = new GFilterConditionView(condition, logicsSupplier, this, () -> columns, toolsVisible, readSelectedValue);
            conditionViews.put(condition, conditionView);

            addConditionView(condition, conditionView);
            conditionView.initView();

            updateConditionsLastState();

            logicsSupplier.getForm().getFormLayout().update(-1);

            conditionView.focusOnValue();

            if (keyEvent != null) {
                conditionView.startEditing(keyEvent);
            }
        }
    }
    
    private void addConditionView(GPropertyFilter condition, GFilterConditionView conditionView) {
        if (condition.filter.container == null) {
            getFiltersContainer().add(condition.filter);
        }
        GFormLayout layout = logicsSupplier.getForm().getFormLayout();
        layout.addBaseComponent(condition.filter, conditionView, null);
    }

    private void removeConditionView(GPropertyFilter condition) {
        GFormLayout layout = logicsSupplier.getForm().getFormLayout();
        layout.removeBaseComponent(condition.filter);
        getFiltersContainer().removeFromChildren(condition.filter);
    }

    @Override
    public void removeCondition(GPropertyFilter condition) {
        conditionViews.remove(condition);

        removeConditionView(condition);

        updateConditionsLastState();

        applyFilters(true);
    }

    public void resetAllConditions() {
        resetAllConditions(true);
    }

    public void resetAllConditions(boolean focusFirstComponent) {
        for (GPropertyFilter filter : new LinkedHashMap<>(conditionViews).keySet()) {
            if (filter.isFixed()) {
                conditionViews.get(filter).clearValueView();
            } else {
                removeConditionView(filter);
                conditionViews.remove(filter);
            }
        }
        applyFilters(new ArrayList<>(), focusFirstComponent);
    }

    private void updateConditionsLastState() {
        int i = 0;
        for (GFilterConditionView cView : conditionViews.values()) {
            i++;
            cView.setLast(i == conditionViews.size());
        }
    }

    public void applyFilters(boolean focusFirstComponent) {
        ArrayList<GPropertyFilter> result = new ArrayList<>();
        for (Map.Entry<GPropertyFilter, GFilterConditionView> entry : conditionViews.entrySet()) {
            GFilterConditionView conditionView = entry.getValue();
            if (!entry.getKey().nullValue() || conditionView.allowNull) {
                result.add(entry.getKey());
                conditionView.setApplied(true);
            } else {
                conditionView.setApplied(false);
            }
            conditionView.isConfirmed = true;
        }
        applyFilters(result, focusFirstComponent);
    }

    public void update() {
        if (initialFilters != null) {
            for (GFilter filter : initialFilters) {
                if (filter.container != null) { // removed in design
                    filter.fixed = true;
                    addCondition(filter, logicsSupplier.getSelectedColumnKey(), false);
                }
            }
            initialFilters = null;
        }

        columns.clear();
        for (Pair<Column, String> column : logicsSupplier.getSelectedColumns()) {
            columns.put(column.first, column.second);
        }
    }

    public void quickEditFilter(Event keyEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        Pair<GPropertyDraw, GGroupObjectValue> column = getActualColumn(propertyDraw, columnKey);

        GFilterConditionView columnCondition = null;
        if (!conditionViews.isEmpty()) {
            for (GPropertyFilter filter : conditionViews.keySet()) {
                if (filter.property.equals(column.first) && GwtSharedUtils.nullEquals(filter.columnKey, column.second)) {
                    columnCondition = conditionViews.get(filter);
                    break;
                }
            }
        }

        if (columnCondition == null) {
            addCondition(propertyDraw, columnKey, keyEvent, false, true);
        } else {
            columnCondition.startEditing(keyEvent);
        }
    }

    public boolean hasFiltersContainer() {
        return hasFiltersContainer;
    }

    public void setVisible(boolean visible) {
        for(GFilterConditionView conditionView : conditionViews.values())
            conditionView.setVisible(visible);
    }
    
    public boolean hasConditions() {
        return !conditionViews.isEmpty();
    }

    public abstract void applyFilters(ArrayList<GPropertyFilter> conditions, boolean focusFirstComponent);
    public abstract void addBinding(GInputEvent event, GBindingEnv env, GFormController.BindingExec pressed, Widget component);
}
