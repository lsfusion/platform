package platform.client.form.queries;

import platform.client.FlatRolloverButton;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Map;

public abstract class QueryView extends JPanel {
    public static final String REMOVE_ALL_ACTION = "removeAll";

    private final static Dimension iconButtonDimension = new Dimension(20, 20);

    private final JPanel condContainer;

    private final JButton applyButton;
    private final JButton addCondition;
    private final JButton collapseButton;

    // при помощи listener идет общение с контроллером
    // выделен в отдельный интерфейс, а не внутренним классом, поскольку от QueryView идет наследование
    private QueryListener listener;

    void setListener(QueryListener listener) {
        this.listener = listener;
    }

    QueryView() {

        setAlignmentY(Component.TOP_ALIGNMENT);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        add(buttons);

        applyButton = new FlatRolloverButton("");
        applyButton.setFocusable(false);
        applyButton.setMinimumSize(iconButtonDimension);
        applyButton.setPreferredSize(iconButtonDimension);
        applyButton.setMaximumSize(iconButtonDimension);
        applyButton.setIcon(getApplyIcon());
        applyButton.setVisible(false);
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (listener != null)
                    listener.applyPressed();
            }
        });
        buttons.add(applyButton);

        Component centerGlue = Box.createHorizontalGlue();
        buttons.add(centerGlue);

        addCondition = new FlatRolloverButton("");
        addCondition.setFocusable(false);
        addCondition.setMinimumSize(iconButtonDimension);
        addCondition.setPreferredSize(iconButtonDimension);
        addCondition.setMaximumSize(iconButtonDimension);
        addCondition.setIcon(getAddConditionIcon());
        addCondition.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                if (listener != null)
                    listener.addConditionPressed(false);
            }
        });

        buttons.add(addCondition);

        collapseButton = new FlatRolloverButton();
        collapseButton.setFocusable(false);
        collapseButton.setPreferredSize(iconButtonDimension);
        collapseButton.setMaximumSize(iconButtonDimension);
        collapseButton.setMinimumSize(iconButtonDimension);
        collapseButton.setVisible(false);
        collapseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setCollapsed(!collapsed);
            }
        });

        buttons.add(collapseButton);

        condContainer = new JPanel();
        condContainer.setLayout(new BoxLayout(condContainer, BoxLayout.Y_AXIS));
        add(condContainer);

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getEnter(), "applyQuery");
        getActionMap().put("applyQuery", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {

                // останавливаем ввод, чтобы записалось правильное значение в ClientPropertyFilter
                for (ClientPropertyFilter filter : condViews.keySet()) {
                    condViews.get(filter).stopEditing();
                }
                listener.applyPressed();
            }
        });

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getRemoveFiltersKeyStroke(), REMOVE_ALL_ACTION);

        addActions(this);
    }

    // используется для того, чтобы во внешнем компоненте по нажатии кнопок можно было создать отбор/поиск
    public void addActions(JComponent comp) {
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(0), "newFilter");
        comp.getActionMap().put("newFilter", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                if (listener != null)
                    listener.addConditionPressed(true);
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(InputEvent.ALT_DOWN_MASK), "addFilter");
        comp.getActionMap().put("addFilter", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                if (listener != null)
                    listener.addConditionPressed(false);
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(InputEvent.SHIFT_DOWN_MASK), REMOVE_ALL_ACTION);
        comp.getActionMap().put(REMOVE_ALL_ACTION, new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                listener.allConditionsRemoved();
                listener.applyPressed();
            }
        });
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public void updateUI() {

        if (condContainer != null)
            condContainer.updateUI();

        if (applyButton != null)
            applyButton.updateUI();

        if (addCondition != null)
            addCondition.updateUI();

        if (collapseButton != null)
            collapseButton.updateUI();
    }

    // действия, вызываемые контроллером
    void queryApplied() {

        applyButton.setVisible(false);

        validate();
    }

    // используется для того, чтобы удалять условия запросов
    private final Map<ClientPropertyFilter, QueryConditionView> condViews = new HashMap<ClientPropertyFilter, QueryConditionView>();

    void addConditionView(ClientPropertyFilter condition, GroupObjectLogicsSupplier logicsSupplier) {

        QueryConditionView condView = new QueryConditionView(condition, logicsSupplier) {

            protected void conditionChanged() {
                conditionsChanged();
            }

            protected void conditionRemoved(ClientPropertyFilter condition) {
                if (listener != null)
                    listener.conditionRemoved(condition);
            }
        };
        condContainer.add(condView);

        condViews.put(condition, condView);

        setCollapsed(false);
        conditionsChanged();

        // сразу становимся на ввод значения
        condView.requestValueFocus();
    }

    void removeCondition(ClientPropertyFilter condition) {

        condContainer.remove(condViews.get(condition));
        condViews.remove(condition);

        if (condViews.isEmpty()) {
           listener.conditionsUpdated();
        }
        conditionsChanged();
    }

    void removeAllConditions() {

        condContainer.removeAll();
        condViews.clear();

        listener.conditionsUpdated();
        conditionsChanged();
    }

    public int getVisibleConditionsCount() {
        return condContainer.isVisible() ? condViews.size() : 0;
    }

    void conditionsChanged() {

        applyButton.setVisible(true);

        collapseButton.setVisible(condViews.size() > 0);

        getParent().getParent().validate();
        revalidate();
    }

    // сворачивание/разворачивание отбора
    private final ImageIcon collapseIcon = new ImageIcon(getClass().getResource("/images/collapse.gif"));
    private final ImageIcon expandIcon = new ImageIcon(getClass().getResource("/images/expand.gif"));

    private boolean collapsed = false;

    void setCollapsed(boolean collapsed) {

        this.collapsed = collapsed;
        if (!collapsed) {
            collapseButton.setIcon(collapseIcon);
            condContainer.setVisible(true);
            listener.conditionsUpdated();
        } else {
            collapseButton.setIcon(expandIcon);
            condContainer.setVisible(false);
            listener.conditionsUpdated();
        }
    }

    protected abstract Icon getApplyIcon();

    protected abstract Icon getAddConditionIcon();

    protected abstract KeyStroke getKeyStroke(int modifier);

    public void startEditing(ClientPropertyDraw propertyDraw) {
        if (condViews.size() > 0) {
            QueryConditionView view = condViews.values().iterator().next();
            view.setSelectedPropertyDraw(propertyDraw);
            view.startEditing();
        }
    }

    public void stopEditing() {
        for (QueryConditionView view : condViews.values()) {
            view.stopEditing();
        }
    }
}