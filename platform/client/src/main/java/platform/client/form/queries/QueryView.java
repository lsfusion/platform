package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.filter.ClientPropertyFilter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public abstract class QueryView extends JPanel {

    private final static Dimension iconButtonDimension = new Dimension(22,22);

    private final JPanel buttons;
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

        setBorder(new EmptyBorder(0,0,0,0));

        buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        add(buttons);

        applyButton = new JButton("");
        applyButton.setFocusable(false);
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

        addCondition = new JButton("");
        addCondition.setFocusable(false);
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

        collapseButton = new JButton();
        collapseButton.setFocusable(false);
        collapseButton.setPreferredSize(iconButtonDimension);
        collapseButton.setMaximumSize(iconButtonDimension);
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

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "applyQuery");
        getActionMap().put("applyQuery", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {

                // останавливаем ввод, чтобы записалось правильное значение в ClientPropertyFilter
                for (ClientPropertyFilter filter : condViews.keySet()) {
                    condViews.get(filter).stopEditing();
                }
                listener.applyPressed();
            }
        });

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK), "removeAll");
        getActionMap().put("removeAll", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                listener.allConditionsRemoved();
                listener.applyPressed();
            }
        });

    }

    // используется для того, чтобы во внешнем компоненте по нажатии кнопок можно было создать отбор/поиск
    public void addActions(JComponent comp) {

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(getKeyEvent(), 0), "newFilter");
        comp.getActionMap().put("newFilter", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                if (listener != null)
                    listener.addConditionPressed(true);
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(getKeyEvent(), InputEvent.ALT_DOWN_MASK), "addFilter");
        comp.getActionMap().put("addFilter", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                if (listener != null)
                    listener.addConditionPressed(false);
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

        conditionsChanged();
    }

    void removeAllConditions() {

        condContainer.removeAll();
        condViews.clear();

        conditionsChanged();
    }


    void conditionsChanged() {

        applyButton.setVisible(true);

        collapseButton.setVisible(condViews.size() > 0);

        getParent().validate();
//        validate();
    }

    // сворачивание/разворачивание отбора
    private final ImageIcon collapseIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/collapse.gif"));
    private final ImageIcon expandIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/expand.gif"));

    private boolean collapsed = false;

    void setCollapsed(boolean collapsed) {

        this.collapsed = collapsed;
        if (!collapsed) {
            collapseButton.setIcon(collapseIcon);
            condContainer.setVisible(true);
        } else {
            collapseButton.setIcon(expandIcon);
            condContainer.setVisible(false);
        }
    }

    protected abstract Icon getApplyIcon();
    protected abstract Icon getAddConditionIcon();
    protected abstract int getKeyEvent();

}