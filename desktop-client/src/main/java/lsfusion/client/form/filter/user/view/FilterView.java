package lsfusion.client.form.filter.user.view;

import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.user.controller.FilterController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.panel.view.DataPanelView;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.base.view.FlexLayout;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.Map;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.interop.base.view.FlexAlignment.CENTER;
import static lsfusion.interop.base.view.FlexAlignment.START;
import static lsfusion.interop.form.event.KeyStrokes.getFilterKeyStroke;

public class FilterView extends FlexPanel implements FilterConditionView.UIHandler {
    public static final String ADD_ICON_PATH = "filtadd.png";
    public static final String RESET_ICON_PATH = "filtreset.png";
    public static final String FILTER_ICON_PATH = "filt.png";

    private final FilterController controller;
    private ClientFilter filterComponent;
    private boolean initialized = false;

    private JButton addConditionButton;
    private JButton resetConditionsButton;

    private JPanel buttonPanel;
    private JPanel condContainer;

    private final Map<ClientPropertyFilter, FilterConditionView> conditionViews = new LinkedHashMap<>();

    private boolean toolsVisible;

    public FilterView(FilterController controller, ClientFilter filerComponent) {
        super(false, CENTER);
        this.controller = controller;
        this.filterComponent = filerComponent;

        initButtons();

        initLayout();

        setFocusable(false);

        initUIHandlers();
    }

    private void initButtons() {
        addConditionButton = new ToolbarGridButton(ADD_ICON_PATH, getString("form.queries.filter.add.condition"));
        addConditionButton.addActionListener(ae -> addCondition(KeyStrokes.createAddUserFilterKeyEvent(this)));
        addConditionButton.setVisible(toolsVisible);

        resetConditionsButton = new ToolbarGridButton(RESET_ICON_PATH, getString("form.queries.filter.reset.conditions"));
        resetConditionsButton.addActionListener(e -> removeAllConditions());
        resetConditionsButton.setVisible(toolsVisible);
    }

    private void initLayout() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlexLayout(buttonPanel, false, START));

        buttonPanel.add(addConditionButton, new FlexConstraints(CENTER, 0));
        buttonPanel.add(Box.createHorizontalStrut(2), new FlexConstraints(CENTER, 0));
        buttonPanel.add(resetConditionsButton, new FlexConstraints(CENTER, 0));

        condContainer = new JPanel();
        condContainer.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        condContainer.setLayout(new FlexLayout(condContainer, false, START));

        add(condContainer, new FlexConstraints(CENTER, 1));
        add(buttonPanel, new FlexConstraints(CENTER, 0));
    }

    private void initUIHandlers() {
        addActionsToInputMap(this);

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getEnter(), "applyQuery");
        getActionMap().put("applyQuery", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                RmiQueue.runAction(() -> applyFilters(true));
            }
        });

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getRemoveFiltersKeyStroke(), "removeAll");
    }

    // используется для того, чтобы во внешнем компоненте по нажатии кнопок можно было создать отбор/поиск
    public void addActionsToInputMap(JComponent comp) {
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.ALT_DOWN_MASK), "newFilter");
        comp.getActionMap().put("newFilter", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                if (!controller.getLogicsSupplier().getFormController().isEditing()) {
                    SwingUtilities.invokeLater(() -> addCondition(ae, true));
                }
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(0), "addFilter");
        comp.getActionMap().put("addFilter", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                if (!controller.getLogicsSupplier().getFormController().isEditing()) {
                    addCondition(ae);
                }
            }
        });

        comp.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStrokes.getRemoveFiltersKeyStroke(), "removeAll");
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.SHIFT_DOWN_MASK), "removeAll");
        comp.getActionMap().put("removeAll", createRemoveAllAction());
    }

    public void addActionsToPanelInputMap(final JComponent comp) {
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getFilterKeyStroke(InputEvent.ALT_DOWN_MASK), "newFilter");
        comp.getActionMap().put("newFilter", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                if (comp instanceof DataPanelView && !controller.getLogicsSupplier().getFormController().isEditing()) {
                    SwingUtilities.invokeLater(() -> addCondition(ae, true, true));
                }
            }
        });

        //кто-то съедает pressed F2, поэтому ловим released
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0, true), "addFilter");
        comp.getActionMap().put("addFilter", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                if (!controller.getLogicsSupplier().getFormController().isEditing()) {
                    addCondition(ae, false, true);
                }
            }
        });

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
                if (!controller.getLogicsSupplier().getFormController().isEditing()) {
                    RmiQueue.runAction(() -> removeAllConditions());
                }
            }
        };
    }

    public void updateUI() {
        if (condContainer != null)
            condContainer.updateUI();

        if (addConditionButton != null)
            addConditionButton.updateUI();
        
        if (resetConditionsButton != null)
            resetConditionsButton.updateUI();
    }

    public boolean isToolsVisible() {
        return toolsVisible;
    }

    public void toggleToolsVisible() {
        toolsVisible = !toolsVisible;
        
        for (FilterConditionView view : conditionViews.values()) {
            view.setToolsVisible(toolsVisible);
        }

        addConditionButton.setVisible(toolsVisible);
        resetConditionsButton.setVisible(toolsVisible);
    }

    public void addCondition(EventObject keyEvent) {
        addCondition(keyEvent, false);
    }

    public void addCondition(EventObject keyEvent, boolean replace) {
        addCondition(keyEvent, replace, true);
    }

    public void addCondition(EventObject keyEvent, boolean replace, boolean readSelectedValue) {
        addCondition(null, null, keyEvent, replace, readSelectedValue);
    }
    
    public void addCondition(ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey, boolean readSelectedValue) {
        addCondition(propertyDraw, columnKey, false, readSelectedValue);
    }

    public void addCondition(ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey, boolean replace, boolean readSelectedValue) {
        addCondition(propertyDraw, columnKey, null, replace, readSelectedValue);
    }

    public void addCondition(ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey, EventObject keyEvent, boolean replace, boolean readSelectedValue) {
        if (replace) {
            // считаем, что в таком случае просто нажали сначала все удалить, а затем - добавить
            removeAllConditions(false);
        }
        
        ClientPropertyFilter condition = controller.getNewCondition(propertyDraw, columnKey);
        if (condition != null) {
            addCondition(condition, controller.getLogicsSupplier(), keyEvent, readSelectedValue);
        }
    }

    public void addCondition(ClientPropertyFilter condition, TableController logicsSupplier, EventObject keyEvent, boolean readSelectedValue) {
        logicsSupplier.getFormController().commitOrCancelCurrentEditing();
        
        FilterConditionView condView = new FilterConditionView(condition, logicsSupplier, this, toolsVisible, readSelectedValue);
        conditionViews.put(condition, condView);

        condContainer.add(condView, new FlexConstraints(CENTER, 0));

        updateConditionsLastState();

        condContainer.revalidate();
        condContainer.repaint();

        if (keyEvent != null) {
            condView.startEditing(keyEvent);
        }
    }

    public void removeAllConditions() {
        removeAllConditions(true);
    }

    public void removeAllConditions(boolean focusFirstComponent) {
        controller.removeAllConditions(focusFirstComponent);
        conditionViews.clear();

        condContainer.removeAll();
        condContainer.revalidate();
        condContainer.repaint();
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension ms = super.getMinimumSize();
        if (ms.width < 1 || ms.height < 1) {
            return new Dimension(Math.max(1, ms.width), Math.max(1, ms.height));
        } else {
            return ms;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension ps = super.getPreferredSize();
        if (ps.width < 1 || ps.height < 1) {
            return new Dimension(Math.max(1, ps.width), Math.max(1, ps.height));
        } else {
            return ps;
        }
    }

    public void updateConditionsLastState() {
        int i = 0;
        for (FilterConditionView cView : conditionViews.values()) {
            i++;
            cView.setLast(i == conditionViews.size());
        }
    }

    @Override
    public void removeCondition(ClientPropertyFilter condition) {
        FilterConditionView view = conditionViews.get(condition);
        if (view != null) {
            condContainer.remove(view);
            conditionViews.remove(condition);

            updateConditionsLastState();

            condContainer.revalidate();
            condContainer.repaint();

            applyFilters(true);
        }
    }

    @Override
    public void applyFilters(boolean focusFirstComponent) {
        ArrayList<ClientPropertyFilter> result = new ArrayList<>();
        for (Map.Entry<ClientPropertyFilter, FilterConditionView> entry : conditionViews.entrySet()) {
            if (entry.getValue().allowNull || !entry.getKey().nullValue()) {
                result.add(entry.getKey());
                
                entry.getValue().isApplied = true;
            }
        }
        
        controller.applyFilters(result, focusFirstComponent);
    }

    public void update() {
        if (!initialized) {
            for (ClientPropertyDraw property : filterComponent.properties) {
                addCondition(property, controller.getLogicsSupplier().getSelectedColumn(), false);
            }
            
            initialized = true;
        }
    }
}