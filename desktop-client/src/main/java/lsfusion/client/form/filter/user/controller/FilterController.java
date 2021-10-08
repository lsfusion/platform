package lsfusion.client.form.filter.user.controller;

import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.ClientContainerView;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.user.view.FilterConditionView;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.panel.view.DataPanelView;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.interop.form.event.KeyStrokes.getFilterKeyStroke;

public abstract class FilterController implements FilterConditionView.UIHandler {
    public static final String ADD_ICON_PATH = "filtadd.png";
    public static final String RESET_ICON_PATH = "filtreset.png";
    public static final String FILTER_ICON_PATH = "filt.png";
    
    private final TableController logicsSupplier;
    
    private final ToolbarGridButton toolbarButton;
    private ToolbarGridButton addConditionButton;
    private ToolbarGridButton resetConditionsButton;

    private JComponent filtersContainerComponent;

    private final Map<ClientPropertyFilter, FilterConditionView> conditionViews = new LinkedHashMap<>();

    private List<ClientFilter> initialFilters;
    
    private boolean toolsVisible;
    
    public FilterController(TableController logicsSupplier, List<ClientFilter> filters, ClientContainerView filtersContainer) {
        this.logicsSupplier = logicsSupplier;
        this.initialFilters = filters;
        if (filtersContainer != null) {
            filtersContainerComponent = filtersContainer.getView().getComponent();
        }

        toolbarButton = new ToolbarGridButton(FILTER_ICON_PATH, getString("form.queries.filter.tools.show"));
        toolbarButton.addActionListener(ae -> {
            toggleToolsVisible();
            toolbarButton.setToolTipText(toolsVisible ? getString("form.queries.filter.tools.hide") : getString("form.queries.filter.tools.show"));
            toolbarButton.showBackground(toolsVisible);
        });

        if (hasOwnContainer()) {
            addConditionButton = new ToolbarGridButton(ADD_ICON_PATH, getString("form.queries.filter.add.condition"));
            addConditionButton.addActionListener(ae -> addCondition());
            addConditionButton.setVisible(false);
        }

        resetConditionsButton = new ToolbarGridButton(RESET_ICON_PATH, getString("form.queries.filter.reset.conditions"));
        resetConditionsButton.addActionListener(e -> removeAllConditions());
        resetConditionsButton.setVisible(false);

        if (filtersContainerComponent != null) {
            filtersContainerComponent.setFocusable(false);
        }

        initUIHandlers();
    }

    public JButton getToolbarButton() {
        return toolbarButton;
    }

    public ToolbarGridButton getAddFilterConditionButton() {
        return addConditionButton;
    }

    public ToolbarGridButton getResetFiltersButton() {
        return resetConditionsButton;
    }

    private void initUIHandlers() {
        if (filtersContainerComponent != null) {
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
        if (hasOwnContainer()) {
            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.ALT_DOWN_MASK), "newFilter");
            comp.getActionMap().put("newFilter", new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    if (!logicsSupplier.getFormController().isEditing()) {
                        SwingUtilities.invokeLater(() -> addCondition(ae, true));
                    }
                }
            });

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(0), "addFilter");
            comp.getActionMap().put("addFilter", new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    if (!logicsSupplier.getFormController().isEditing()) {
                        addCondition(ae);
                    }
                }
            });
        }

        comp.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStrokes.getRemoveFiltersKeyStroke(), "removeAll");
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.SHIFT_DOWN_MASK), "removeAll");
        comp.getActionMap().put("removeAll", createRemoveAllAction());
    }

    public void addActionsToPanelInputMap(final JComponent comp) {
        if (hasOwnContainer()) {
            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.ALT_DOWN_MASK), "newFilter");
            comp.getActionMap().put("newFilter", new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    if (comp instanceof DataPanelView && !logicsSupplier.getFormController().isEditing()) {
                        SwingUtilities.invokeLater(() -> addCondition(ae, true, true));
                    }
                }
            });

            //кто-то съедает pressed F2, поэтому ловим released
            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0, true), "addFilter");
            comp.getActionMap().put("addFilter", new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    if (!logicsSupplier.getFormController().isEditing()) {
                        addCondition(ae, false, true);
                    }
                }
            });
        }

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.SHIFT_DOWN_MASK), "removeAll");
        comp.getActionMap().put("removeAll", createRemoveAllAction());
    }

    private AbstractAction createRemoveAllAction() {
        return new AbstractAction() {
            @Override
            public boolean isEnabled() {
                return !conditionViews.isEmpty();
            }

            public void actionPerformed(ActionEvent ae) {
                if (!logicsSupplier.getFormController().isEditing()) {
                    RmiQueue.runAction(() -> removeAllConditions());
                }
            }
        };
    }

    public void toggleToolsVisible() {
        toolsVisible = !toolsVisible;

        if (!conditionViews.isEmpty()) {
            for (FilterConditionView view : conditionViews.values()) {
                view.setToolsVisible(toolsVisible);
            }
        } else if (toolsVisible) {
            addCondition();
        }

        if (addConditionButton != null) {
            addConditionButton.setVisible(toolsVisible);
        }
        resetConditionsButton.setVisible(toolsVisible);
    }

    public ClientContainer getFiltersContainer() {
        return logicsSupplier.getFiltersContainer();
    }

    public ClientPropertyFilter getNewCondition(ClientFilter filter, ClientGroupObjectValue columnKey) {
        ClientPropertyDraw filterProperty = filter != null ? filter.property: null;
        ClientGroupObjectValue filterColumnKey = columnKey;

        if (filterProperty == null) {
            filterProperty = logicsSupplier.getSelectedProperty();
            if (filterProperty != null) {
                filterColumnKey = logicsSupplier.getSelectedColumn();
            }
        }

        if (filterProperty == null) {
            return null;
        }

        if (filter == null) {
            filter = new ClientFilter(filterProperty);
        } else if (filter.property == null) {
            filter.property = filterProperty;
        }

        return new ClientPropertyFilter(filter, logicsSupplier.getSelectedGroupObject(), filterColumnKey, null);
    }

    public void addCondition() {
        if (hasOwnContainer()) {
            addCondition(KeyStrokes.createAddUserFilterKeyEvent(filtersContainerComponent));
        }
    }

    public void addCondition(EventObject keyEvent) {
        addCondition(keyEvent, false);
    }

    public void addCondition(EventObject keyEvent, boolean replace) {
        addCondition(keyEvent, replace, true);
    }

    public void addCondition(EventObject keyEvent, boolean replace, boolean readSelectedValue) {
        addCondition((ClientFilter) null, null, keyEvent, replace, readSelectedValue);
    }

    public void addCondition(ClientFilter filter, ClientGroupObjectValue columnKey, boolean readSelectedValue) {
        addCondition(filter, columnKey, null, false, readSelectedValue);
    }

    public void addCondition(ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey, EventObject keyEvent, boolean replace, boolean readSelectedValue) {
        addCondition(new ClientFilter(propertyDraw), columnKey, keyEvent, replace, readSelectedValue);
    }

    public void addCondition(ClientFilter filter, ClientGroupObjectValue columnKey, EventObject keyEvent, boolean replace, boolean readSelectedValue) {
        if (replace) {
            // считаем, что в таком случае просто нажали сначала все удалить, а затем - добавить
            removeAllConditions(false);
        }

        ClientPropertyFilter condition = getNewCondition(filter, columnKey);
        if (condition != null) {
            addCondition(condition, logicsSupplier, keyEvent, readSelectedValue);
        }
    }

    public void addCondition(ClientPropertyFilter condition, TableController logicsSupplier, EventObject keyEvent, boolean readSelectedValue) {
        logicsSupplier.getFormController().commitOrCancelCurrentEditing();

        FilterConditionView condView = new FilterConditionView(condition, logicsSupplier, this, toolsVisible, readSelectedValue);
        conditionViews.put(condition, condView);

        if (condition.filter.container == null) { // added by user
            getFiltersContainer().add(condition.filter);
        }
        ClientFormLayout layout = logicsSupplier.getFormController().getLayout();
        layout.addBaseComponent(condition.filter, condView);

        updateConditionsLastState();

        layout.autoShowHideContainers();

        if (keyEvent != null) {
            condView.startEditing(keyEvent);
        }
    }

    private void removeConditionView(ClientPropertyFilter condition) {
        ClientFormLayout layout = logicsSupplier.getFormController().getLayout();
        layout.removeBaseComponent(condition.filter, conditionViews.get(condition));
        getFiltersContainer().removeFromChildren(condition.filter);
    }

    @Override
    public void removeCondition(ClientPropertyFilter condition) {
        removeConditionView(condition);
        conditionViews.remove(condition);

        updateConditionsLastState();
        applyFilters(true);
    }

    public void removeAllConditions() {
        removeAllConditions(true);
    }

    public void removeAllConditions(boolean focusFirstComponent) {
        applyFilters(Collections.emptyList(), focusFirstComponent);

        for (ClientPropertyFilter filter : conditionViews.keySet()) {
            removeConditionView(filter);
        }

        conditionViews.clear();
    }

    public void updateConditionsLastState() {
        int i = 0;
        for (FilterConditionView cView : conditionViews.values()) {
            i++;
            cView.setLast(i == conditionViews.size());
        }
    }

    public void applyFilters(boolean focusFirstComponent) {
        ArrayList<ClientPropertyFilter> result = new ArrayList<>();
        for (Map.Entry<ClientPropertyFilter, FilterConditionView> entry : conditionViews.entrySet()) {
            if (entry.getValue().allowNull || !entry.getKey().nullValue()) {
                result.add(entry.getKey());
            }
            entry.getValue().isConfirmed = true;
        }

        applyFilters(result, focusFirstComponent);
    }

    public abstract void applyFilters(List<ClientPropertyFilter> conditions, boolean focusFirstComponent);

    public void update() {
        if (initialFilters != null) {
            for (ClientFilter filter : initialFilters) {
                if (filter.container != null) { // removed in design
                    addCondition(filter, logicsSupplier.getSelectedColumn(), false);
                }
            }

            initialFilters = null;
        }
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        addCondition(propertyDraw, columnKey, initFilterKeyEvent, true, true);
    }
    
    public boolean hasOwnContainer() {
        return filtersContainerComponent != null;
    }

    public void setVisible(boolean visible) {
        if (filtersContainerComponent != null) {
            filtersContainerComponent.setVisible(visible);
        }
    }
}
