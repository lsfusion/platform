package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.interop.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class QueryView extends JPanel implements QueryConditionView.UIHandlers {
    private JButton applyButton;
    private JButton addCondButton;

    private JPanel buttonPanel;
    private JPanel condContainer;

    // при помощи listener идет общение с контроллером
    // выделен в отдельный интерфейс, а не внутренним классом, поскольку от QueryView идет наследование
    private final QueryController controller;

    // используется для того, чтобы удалять условия запросов
    private final Map<ClientPropertyFilter, QueryConditionView> condViews = new LinkedHashMap<ClientPropertyFilter, QueryConditionView>();

    QueryView(QueryController ilistener) {
        this.controller = ilistener;

        setLayout(new BorderLayout());

        initButtons();

        initLayout();

        setFocusable(false);
        setContentVisible(false);

        initUIHandlers();
    }

    private void initButtons() {
        applyButton = new ToolbarGridButton(getApplyIcon(), null);
        applyButton.setVisible(false);
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                controller.applyPressed();
            }
        });

        addCondButton = new ToolbarGridButton(getAddConditionIcon(), null);
        addCondButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                controller.addConditionPressed();
            }
        });
    }

    private void initLayout() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(applyButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(addCondButton);

        condContainer = new JPanel();
        condContainer.setLayout(new BoxLayout(condContainer, BoxLayout.Y_AXIS));

        add(buttonPanel, BorderLayout.NORTH);
        add(condContainer, BorderLayout.CENTER);
    }

    private void initUIHandlers() {
        addActionsToInputMap(this);

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getEnter(), "applyQuery");
        getActionMap().put("applyQuery", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                controller.applyPressed();
            }
        });

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getRemoveFiltersKeyStroke(), "removeAll");
    }

    // используется для того, чтобы во внешнем компоненте по нажатии кнопок можно было создать отбор/поиск
    public void addActionsToInputMap(JComponent comp) {
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(0), "newFilter");
        comp.getActionMap().put("newFilter", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                controller.replaceConditionPressed();
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(InputEvent.ALT_DOWN_MASK), "addFilter");
        comp.getActionMap().put("addFilter", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                controller.addConditionPressed();
            }
        });

        comp.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStrokes.getRemoveFiltersKeyStroke(), "removeAll");
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(InputEvent.SHIFT_DOWN_MASK), "removeAll");
        comp.getActionMap().put("removeAll", new AbstractAction() {
            @Override
            public boolean isEnabled() {
                return controller.hasAnyFilter();
            }

            public void actionPerformed(ActionEvent ae) {
                controller.allRemovedPressed();
            }
        });
    }

    public void updateUI() {
        if (condContainer != null)
            condContainer.updateUI();

        if (applyButton != null)
            applyButton.updateUI();

        if (addCondButton != null)
            addCondButton.updateUI();
    }

    // действия, вызываемые контроллером
    void addCondition(ClientPropertyFilter condition, GroupObjectLogicsSupplier logicsSupplier) {
        QueryConditionView condView = new QueryConditionView(condition, logicsSupplier, this);
        condViews.put(condition, condView);

        condContainer.add(condView);

        conditionChanged();

        // сразу становимся на ввод значения
        condView.requestValueFocus();
    }

    void removeCondition(ClientPropertyFilter condition) {
        condContainer.remove(condViews.get(condition));
        condViews.remove(condition);

        conditionChanged();
    }

    void removeAllConditions() {
        condContainer.removeAll();
        condViews.clear();

        conditionChanged();
    }

    public void setContentVisible(boolean visible) {
        // ХАК: свинг не хочет нормально проталкивать нажатие клавиши в невидимые компоненты,
        // поэтому если если компонент фильтра сркыт, то быстрая фильтрация при вводе не работает..
        // поэтому контейнер фильтра всегда видимый, если включен режим таблицы и скрывается только при переходе в панель...
        // также SimplexLayout не хочет сразу нормально работать с этой панелью, если все компоненты скрыты, поэтому переопределны getMinimum/Preferred
        buttonPanel.setVisible(visible);
        condContainer.setVisible(visible);
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

    public void startEditing(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw) {
        if (condViews.size() > 0) {
            QueryConditionView view = condViews.values().iterator().next();
            view.setSelectedPropertyDraw(propertyDraw);
            view.startEditing(initFilterKeyEvent);
        }
    }

    void queryApplied() {
        applyButton.setVisible(false);
    }

    @Override
    public void conditionChanged() {
        applyButton.setVisible(true);

        for (QueryConditionView conditionView : condViews.values()) {
            conditionView.setJunctionVisible(Arrays.asList(condViews.values().toArray()).indexOf(conditionView) < condViews.size() - 1);
        }

        controller.dropLayoutCaches();
    }

    @Override
    public void conditionRemoved(ClientPropertyFilter condition) {
        controller.removeConditionPressed(condition);
    }

    protected abstract Icon getApplyIcon();

    protected abstract Icon getAddConditionIcon();

    protected abstract KeyStroke getKeyStroke(int modifier);
}