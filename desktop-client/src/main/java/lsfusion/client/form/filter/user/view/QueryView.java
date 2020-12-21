package lsfusion.client.form.filter.user.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.design.view.JComponentPanel;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.user.FilterView;
import lsfusion.client.form.filter.user.controller.QueryController;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.panel.view.DataPanelView;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class QueryView extends JComponentPanel implements QueryConditionView.UIHandlers {
    private JButton applyButton;
    private JButton addCondButton;

    private JPanel buttonPanel;
    private JPanel condContainer;

    // при помощи listener идет общение с контроллером
    // выделен в отдельный интерфейс, а не внутренним классом, поскольку от QueryView идет наследование
    private final QueryController controller;

    // используется для того, чтобы удалять условия запросов
    private final Map<ClientPropertyFilter, QueryConditionView> condViews = new LinkedHashMap<>();

    public QueryView(QueryController ilistener) {
        this.controller = ilistener;

        initButtons();

        initLayout();

        setFocusable(false);
        setContentVisible(false);

        initUIHandlers();
    }

    private void initButtons() {
        applyButton = new ToolbarGridButton(FilterView.APPLY_ICON_PATH, ClientResourceBundle.getString("form.queries.filter.apply"));
        applyButton.setVisible(false);
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        controller.applyPressed();
                    }
                });
            }
        });

        addCondButton = new ToolbarGridButton(FilterView.ADD_ICON_PATH, ClientResourceBundle.getString("form.queries.filter.add.condition") + " (alt + F2)");
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
        buttonPanel.add(Box.createHorizontalStrut(2));
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
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        controller.applyPressed();
                    }
                });
            }
        });

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getRemoveFiltersKeyStroke(), "removeAll");
    }

    // используется для того, чтобы во внешнем компоненте по нажатии кнопок можно было создать отбор/поиск
    public void addActionsToInputMap(JComponent comp) {
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(InputEvent.ALT_DOWN_MASK), "newFilter");
        comp.getActionMap().put("newFilter", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                controller.replaceConditionPressed();
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(0), "addFilter");
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
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        controller.allRemovedPressed();
                    }
                });
            }
        });
    }

    public void addActionsToPanelInputMap(final JComponent comp) {
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(InputEvent.ALT_DOWN_MASK), "newFilter");
        comp.getActionMap().put("newFilter", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                if(comp instanceof DataPanelView)
                    controller.replaceConditionPressed(((DataPanelView) comp).getProperty(), ((DataPanelView) comp).getColumnKey());
            }
        });

        //кто-то съедает pressed F2, поэтому ловим released
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0, true), "addFilter");
        comp.getActionMap().put("addFilter", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                controller.addConditionPressed(((DataPanelView) comp).getProperty(), ((DataPanelView) comp).getColumnKey());
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getKeyStroke(InputEvent.SHIFT_DOWN_MASK), "removeAll");
        comp.getActionMap().put("removeAll", new AbstractAction() {
            @Override
            public boolean isEnabled() {
                return controller.hasAnyFilter();
            }

            public void actionPerformed(ActionEvent ae) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        controller.allRemovedPressed();
                    }
                });
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
    public void addCondition(ClientPropertyFilter condition, TableController logicsSupplier) {
        QueryConditionView condView = new QueryConditionView(condition, logicsSupplier, this);
        condViews.put(condition, condView);

        condContainer.add(condView);

        conditionChanged();

        // сразу становимся на ввод значения
        condView.requestValueFocus();
    }

    public void removeCondition(ClientPropertyFilter condition) {
        condContainer.remove(condViews.get(condition));
        condViews.remove(condition);

        conditionChanged();
    }

    public void removeAllConditions() {
        condContainer.removeAll();
        condContainer.repaint(); // если повторно нажали F2 с одним условием, на фоне остаётся изображение старого
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

    public void queryApplied() {
        applyButton.setVisible(false);
    }

    @Override
    public void conditionChanged() {
        applyButton.setVisible(true);

        for (QueryConditionView conditionView : condViews.values()) {
            conditionView.setJunctionVisible(Arrays.asList(condViews.values().toArray()).indexOf(conditionView) < condViews.size() - 1);
        }
    }

    @Override
    public void conditionRemoved(ClientPropertyFilter condition) {
        controller.removeConditionPressed(condition);
    }

    @Override
    public void applyQuery() {
        controller.applyPressed();
    }

//    public abstract Icon getApplyIcon();
//
//    public abstract Icon getAddIcon();
//
//    public abstract Icon getFilterIcon();

    protected abstract KeyStroke getKeyStroke(int modifier);
}