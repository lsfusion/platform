package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.filter.ClientPropertyFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

abstract class QueryController {

    public static final ImageIcon collapseIcon = new ImageIcon(QueryView.class.getResource("/images/collapse.png"));
    public static final ImageIcon expandIcon = new ImageIcon(QueryView.class.getResource("/images/expand.png"));

    private enum State {
        HIDDEN, REMOVED, COLLAPSED, EXPANDED
    }

    private final List<ClientPropertyFilter> conditions = new ArrayList<ClientPropertyFilter>();

    private final QueryView view;

    private final GroupObjectLogicsSupplier logicsSupplier;
    private final JButton toolbarButton;

    private State state;
    private State hiddenState;

    QueryController(GroupObjectLogicsSupplier logicsSupplier) {
        this.logicsSupplier = logicsSupplier;

        view = createView();

        toolbarButton = new ToolbarGridButton(view.getAddConditionIcon(), null);
        toolbarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (state == State.COLLAPSED) {
                    setState(State.EXPANDED);
                } else if (state == State.EXPANDED) {
                    setState(State.COLLAPSED);
                } else if (state == State.REMOVED) {
                    replaceConditionPressed();
                }
            }
        });

        setState(State.REMOVED);
    }

    private void setState(State state) {
        this.state = state;

        toolbarButton.setIcon(getStateIcon());

        getView().setContentVisible(state == State.EXPANDED);

        dropLayoutCaches();
    }

    private Icon getStateIcon() {
        switch (state) {
            case REMOVED: return getView().getAddConditionIcon();
            case COLLAPSED: return collapseIcon;
            case EXPANDED: return expandIcon;
        }
        return null;
    }

    public JButton getToolbarButton() {
        return toolbarButton;
    }

    public QueryView getView() {
        return view;
    }

    public List<ClientPropertyFilter> getConditions() {
        return conditions;
    }

    // Здесь слушаем наш View
    public void applyPressed() {
        applyQuery();
    }

    public void replaceConditionPressed() {
        if (addNewCondition(true, null)) {
            setState(State.EXPANDED);
            view.startEditing(null, null);
        }
    }

    public void addConditionPressed() {
        if (addNewCondition(false, null)) {
            setState(State.EXPANDED);
        }
    }

    public void allRemovedPressed() {
        removeAllConditions();
        applyQuery();

        setState(State.REMOVED);
    }

    public void removeConditionPressed(ClientPropertyFilter condition) {
        conditions.remove(condition);

        view.removeCondition(condition);

        if (conditions.isEmpty()) {
            setState(State.REMOVED);
            applyQuery();
        } else {
            setState(State.EXPANDED);
        }
    }

    private boolean addNewCondition(boolean replace, ClientPropertyDraw propertyDraw) {
        ClientPropertyDraw filterProperty = propertyDraw != null ? propertyDraw : logicsSupplier.getSelectedProperty();
        if (filterProperty == null) {
            //не добавляем, если нет ни одного свойства
            return false;
        }

        if (replace) {
            // считаем, что в таком случае просто нажали сначала все удалить, а затем - добавить
            removeAllConditions();
        }

        ClientPropertyFilter filter = new ClientPropertyFilter();
        filter.property = filterProperty;
        filter.groupObject = logicsSupplier.getSelectedGroupObject();

        conditions.add(filter);

        view.addCondition(filter, logicsSupplier);

        return true;
    }

    private void removeAllConditions() {
        conditions.clear();
        view.removeAllConditions();
    }

    private void applyQuery() {
        if (!logicsSupplier.getForm().commitCurrentEditing()) {
            return;
        }

        remoteApplyQuery();

        view.queryApplied();
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw) {
        if (addNewCondition(true, propertyDraw)) {
            setState(State.EXPANDED);
            view.startEditing(initFilterKeyEvent, propertyDraw);
        }
    }

    public boolean hasAnyFilter() {
        return !conditions.isEmpty();
    }

    public void dropLayoutCaches() {
        logicsSupplier.getForm().dropLayoutCaches();
    }

    protected abstract QueryView createView();

    protected abstract void remoteApplyQuery();

    public void setVisible(boolean visible) {
        getView().setVisible(visible);
        if (!visible) {
            if (state != State.HIDDEN) {
                hiddenState = state;
                setState(State.HIDDEN);
            }
        } else {
            if (state == State.HIDDEN) {
                setState(hiddenState);
            }
        }
    }
}
