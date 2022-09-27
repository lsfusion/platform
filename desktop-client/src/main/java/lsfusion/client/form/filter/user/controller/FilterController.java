package lsfusion.client.form.filter.user.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.ClientContainerView;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.filter.user.ClientFilterControls;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.user.view.FilterConditionView;
import lsfusion.client.form.filter.user.view.FilterControlsView;
import lsfusion.client.form.filter.user.view.FiltersHandler;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.panel.view.DataPanelView;
import lsfusion.client.form.view.Column;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.interop.form.event.KeyStrokes.getFilterKeyStroke;

public abstract class FilterController implements FilterConditionView.UIHandler, FiltersHandler {
    public static final String FILTER_ICON_PATH = "filt.png";
    
    private final TableController logicsSupplier;
    private Map<Column, String> columns = new HashMap<>();
    
    private final ToolbarGridButton toolbarButton;
    

    private JComponent filtersContainerComponent;

    private final Map<ClientPropertyFilter, FilterConditionView> conditionViews = new LinkedHashMap<>();

    private List<ClientFilter> initialFilters;
    
    private boolean controlsVisible;
    private FilterControlsView controlsView;
    
    public FilterController(TableController logicsSupplier, List<ClientFilter> filters, ClientContainerView filtersContainer, ClientFilterControls filterControls) {
        this.logicsSupplier = logicsSupplier;
        this.initialFilters = filters;
        if (filtersContainer != null) {
            filtersContainerComponent = filtersContainer.getView().getComponent();
        }

        toolbarButton = new ToolbarGridButton(FILTER_ICON_PATH, getString("form.queries.filter.controls.show"));
        toolbarButton.addActionListener(ae -> {
            toggleControlsVisible();
        });


        if (hasFiltersContainer()) {
            filtersContainerComponent.setFocusable(false);
        }
        
        controlsView = new FilterControlsView(this);
        controlsView.setVisible(controlsVisible);
        logicsSupplier.getFormController().getLayout().addBaseComponent(filterControls, controlsView);

        initUIHandlers();
    }

    public JButton getToolbarButton() {
        return toolbarButton;
    }

    private void initUIHandlers() {
        if (hasFiltersContainer()) {
            addActionsToInputMap(filtersContainerComponent);

            filtersContainerComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getEnter(), "applyQuery");
            filtersContainerComponent.getActionMap().put("applyQuery", new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    RmiQueue.runAction(() -> applyFilters(true));
                }
            });

            filtersContainerComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getRemoveFiltersKeyStroke(), "removeAll");
        }
    }

    // используется для того, чтобы во внешнем компоненте по нажатии кнопок можно было создать отбор/поиск
    public void addActionsToInputMap(JComponent comp) {
        if (hasFiltersContainer()) {
            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.ALT_DOWN_MASK), "newFilter");
            comp.getActionMap().put("newFilter", new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    performNewFilterAction(ae);
                }
            });

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(0), "addFilter");
            comp.getActionMap().put("addFilter", new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    performAddFilterAction(ae);
                }
            });
        }

        comp.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStrokes.getRemoveFiltersKeyStroke(), "removeAll");
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.SHIFT_DOWN_MASK), "removeAll");
        comp.getActionMap().put("removeAll", createResetAllAction());
    }

    public void addActionsToPanelInputMap(final JComponent comp) {
        if (hasFiltersContainer()) {
            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.ALT_DOWN_MASK), "newFilter");
            comp.getActionMap().put("newFilter", new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    if (comp instanceof DataPanelView) {
                        performNewFilterAction(ae);
                    }
                }
            });

            //кто-то съедает pressed F2, поэтому ловим released
            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0, true), "addFilter");
            comp.getActionMap().put("addFilter", new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    performAddFilterAction(ae);
                }
            });
        }

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.SHIFT_DOWN_MASK), "removeAll");
        comp.getActionMap().put("removeAll", createResetAllAction());
    }
    
    private void performNewFilterAction(ActionEvent ae) {
        if (!logicsSupplier.getFormController().isEditing()) {
            SwingUtilities.invokeLater(() -> {
                boolean added = addCondition(ae, true, true);
                if (added) {
                    setControlsVisible(true);
                }
            });
        }
    }
    
    private void performAddFilterAction(ActionEvent ae) {
        if (!logicsSupplier.getFormController().isEditing()) {
            boolean added = addCondition(ae, false, true);
            if (added) {
                setControlsVisible(true);
            }
        }
    }

    public static ActionEvent createAddUserFilterEvent(Component component) {
        return new ActionEvent(component, ActionEvent.ACTION_PERFORMED, "");
    }

    private AbstractAction createResetAllAction() {
        return new AbstractAction() {
            @Override
            public boolean isEnabled() {
                return !conditionViews.isEmpty();
            }

            public void actionPerformed(ActionEvent ae) {
                if (!logicsSupplier.getFormController().isEditing()) {
                    RmiQueue.runAction(() -> resetConditions());
                }
            }
        };
    }

    public void toggleControlsVisible() {
        setControlsVisible(!controlsVisible);
        
        if (conditionViews.isEmpty() && controlsVisible) {
            addCondition();
        }
    }
    
    public void setControlsVisible(boolean visible) {
        controlsVisible = visible;

        if (!conditionViews.isEmpty()) {
            for (FilterConditionView view : conditionViews.values()) {
                view.setControlsVisible(controlsVisible);
            }
        }

        controlsView.setVisible(controlsVisible);
        logicsSupplier.getFormController().getLayout().autoShowHideContainers();
        logicsSupplier.getFormController().revalidate();

        toolbarButton.setToolTipText(controlsVisible ? getString("form.queries.filter.controls.hide") : getString("form.queries.filter.controls.show"));
        toolbarButton.showBackground(controlsVisible);
    }

    public ClientContainer getFiltersContainer() {
        return logicsSupplier.getFiltersContainer();
    }

    public ClientPropertyFilter getNewCondition(ClientFilter filter, ClientGroupObjectValue columnKey) {
        Pair<ClientPropertyDraw, ClientGroupObjectValue> column = getActualColumn(filter != null ? filter.property: null, columnKey);

        if (column.first == null) {
            return null;
        }

        if (filter == null) {
            filter = new ClientFilter(column.first);
        } else if (filter.property == null) {
            filter.property = column.first;
        }

        return new ClientPropertyFilter(filter, logicsSupplier.getSelectedGroupObject(), column.second, null);
    }
    
    private Pair<ClientPropertyDraw, ClientGroupObjectValue> getActualColumn(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        ClientPropertyDraw actualProperty = property;
        ClientGroupObjectValue actualColumnKey = columnKey;

        if (actualProperty == null) {
            actualProperty = logicsSupplier.getSelectedFilterProperty();
            if (actualProperty != null) {
                actualColumnKey = logicsSupplier.getSelectedColumn();
            }
        }
        return Pair.create(actualProperty, actualColumnKey);
    }

    public boolean addCondition() {
        if (hasFiltersContainer()) {
            return addCondition(createAddUserFilterEvent(filtersContainerComponent));
        }
        return false;
    }

    public boolean addCondition(EventObject keyEvent) {
        return addCondition(keyEvent, false);
    }

    public boolean addCondition(EventObject keyEvent, boolean replace) {
        return addCondition(keyEvent, replace, true);
    }

    public boolean addCondition(EventObject keyEvent, boolean replace, boolean readSelectedValue) {
        return addCondition((ClientFilter) null, null, keyEvent, replace, readSelectedValue);
    }

    public boolean addCondition(ClientFilter filter, ClientGroupObjectValue columnKey, boolean readSelectedValue) {
        return addCondition(filter, columnKey, null, false, readSelectedValue);
    }

    public boolean addCondition(ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey, EventObject keyEvent, boolean replace, boolean readSelectedValue) {
        return addCondition(new ClientFilter(propertyDraw), columnKey, keyEvent, replace, readSelectedValue);
    }

    public boolean addCondition(ClientFilter filter, ClientGroupObjectValue columnKey, EventObject keyEvent, boolean replace, boolean readSelectedValue) {
        if (replace) {
            // считаем, что в таком случае просто нажали сначала все удалить, а затем - добавить
            resetAllConditions(false);
        }

        ClientPropertyFilter condition = getNewCondition(filter, columnKey);
        if (condition != null) {
            addCondition(condition, logicsSupplier, keyEvent, readSelectedValue);
            return true;
        }
        return false;
    }

    public void addCondition(ClientPropertyFilter condition, TableController logicsSupplier, EventObject keyEvent, boolean readSelectedValue) {
        logicsSupplier.getFormController().commitOrCancelCurrentEditing();

        FilterConditionView conditionView = new FilterConditionView(condition, logicsSupplier, this, () -> columns, controlsVisible, keyEvent, readSelectedValue);
        conditionViews.put(condition, conditionView);

        addConditionView(condition, conditionView); 
        conditionView.initView();

        updateConditionsLastState();

        logicsSupplier.getFormController().getLayout().autoShowHideContainers();

        if (keyEvent != null) {
            conditionView.startEditing(keyEvent);
        }
    }

    private void addConditionView(ClientPropertyFilter condition, FilterConditionView conditionView) {
        if (condition.filter.container == null) { // added by user
            getFiltersContainer().add(condition.filter);
        }
        ClientFormLayout layout = logicsSupplier.getFormController().getLayout();
        layout.addBaseComponent(condition.filter, conditionView);
    }

    private void removeConditionView(ClientPropertyFilter condition) {
        ClientFormLayout layout = logicsSupplier.getFormController().getLayout();
        layout.removeBaseComponent(condition.filter, conditionViews.get(condition));
        getFiltersContainer().removeFromChildren(condition.filter);
        conditionViews.remove(condition);
        
        if (conditionViews.isEmpty()) {
            setControlsVisible(false);
        }
    }

    @Override
    public void removeCondition(ClientPropertyFilter condition) {
        removeConditionView(condition);

        updateConditionsLastState();
        
        conditionsChanged(true);
    }

    @Override
    public void resetConditions() {
        resetAllConditions(true);
    }

    public void resetAllConditions(boolean focusFirstComponent) {
        for (ClientPropertyFilter filter : new LinkedHashMap<>(conditionViews).keySet()) {
            if (filter.isFixed()) {
                conditionViews.get(filter).clearValueView();
            } else {
                removeConditionView(filter);
                conditionViews.remove(filter);
            }
        }
        applyFilters(focusFirstComponent);
    }

    public void updateConditionsLastState() {
        int i = 0;
        for (FilterConditionView cView : conditionViews.values()) {
            i++;
            cView.setLast(i == conditionViews.size());
        }
    }

    public void conditionsChanged(boolean focusFirstComponent) {
        applyFilters(focusFirstComponent);
    }

    public void applyFilters(boolean focusFirstComponent) {
        ArrayList<ClientPropertyFilter> result = new ArrayList<>();
        for (Map.Entry<ClientPropertyFilter, FilterConditionView> entry : conditionViews.entrySet()) {
            FilterConditionView conditionView = entry.getValue();
            if (!entry.getKey().nullValue() || conditionView.allowNull) {
                result.add(entry.getKey());
                conditionView.setApplied(true);
            } else {
                conditionView.setApplied(false);
            }
        }

        applyFilters(result, focusFirstComponent);
    }

    public abstract void applyFilters(List<ClientPropertyFilter> conditions, boolean focusFirstComponent);

    public void update() {
        if (initialFilters != null) {
            for (ClientFilter filter : initialFilters) {
                if (filter.container != null) { // removed in design
                    filter.fixed = true;
                    addCondition(filter, logicsSupplier.getSelectedColumn(), false);
                }
            }

            initialFilters = null;
        }

        columns.clear();
        for (Pair<Column, String> column : logicsSupplier.getSelectedColumns()) {
            columns.put(column.first, column.second);
        }
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        Pair<ClientPropertyDraw, ClientGroupObjectValue> column = getActualColumn(propertyDraw, columnKey);
        
        FilterConditionView columnCondition = null;
        if (!conditionViews.isEmpty()) {
            for (ClientPropertyFilter filter : conditionViews.keySet()) {
                if (filter.property.equals(column.first) && BaseUtils.nullEquals(filter.columnKey, column.second)) {
                    columnCondition = conditionViews.get(filter);
                    break;
                }
            }
        }
        
        if (columnCondition == null) {
            addCondition(propertyDraw, columnKey, initFilterKeyEvent, false, true);
        } else {
            columnCondition.startEditing(initFilterKeyEvent);
        }
    }
    
    public boolean hasFiltersContainer() {
        return filtersContainerComponent != null;
    }

    public void setVisible(boolean visible) {
        for (FilterConditionView conditionView : conditionViews.values()) {
            SwingUtils.setGridVisible(conditionView, visible);
        }
    }
}
