package lsfusion.client.form.filter.user.controller;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.user.FilterView;
import lsfusion.client.form.filter.user.view.QueryView;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class QueryController {

    public static final String COLLAPSE_ICON_PATH = "collapse.png";

    private enum State {
        HIDDEN, REMOVED, COLLAPSED, EXPANDED
    }

    private final List<ClientPropertyFilter> conditions = new ArrayList<>();

    private final QueryView view;

    private final TableController logicsSupplier;
    private final ToolbarGridButton toolbarButton;

    private State state;
    private State hiddenState;

    public QueryController(TableController logicsSupplier) {
        this.logicsSupplier = logicsSupplier;

        view = createView();

        toolbarButton = new ToolbarGridButton(FilterView.FILTER_ICON_PATH, "") {
            
            @Override
            public String getToolTipText() {
                if (state != null) {
                    switch (state) {
                        case REMOVED:
                            return ClientResourceBundle.getString("form.queries.filter") + " (F2)";
                        case COLLAPSED:
                            return ClientResourceBundle.getString("form.queries.filter.expand");
                        case EXPANDED:
                            return ClientResourceBundle.getString("form.queries.filter.collapse");
                    }
                }
                return null;
            }
        };
        toolbarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                switch (state) {
                    case REMOVED:
                        replaceConditionPressed();
                        toolbarButton.showBackground(false);
                        break;
                    case COLLAPSED: 
                        setState(State.EXPANDED);
                        toolbarButton.showBackground(false);
                        break;
                    case EXPANDED:
                        setState(State.COLLAPSED);
                        toolbarButton.showBackground(true);
                }
            }
        });

        setState(State.REMOVED);
    }

    private void setState(State state) {
        this.state = state;

        toolbarButton.setIconPath(getStateIconPath());

        getView().setContentVisible(state == State.EXPANDED);
    }

    private String getStateIconPath() {
        switch (state) {
            case REMOVED: return FilterView.FILTER_ICON_PATH;
            case COLLAPSED:
            case EXPANDED:
                return COLLAPSE_ICON_PATH;
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
        replaceConditionPressed(null, null);
    }

    public void replaceConditionPressed(ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        if (addNewCondition(true, propertyDraw, columnKey)) {
            setState(State.EXPANDED);
            view.startEditing(null, null);
        }
    }

    public void addConditionPressed() {
       addConditionPressed(null, null);
    }

    public void addConditionPressed(ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        if (addNewCondition(false, propertyDraw, columnKey)) {
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

    private boolean addNewCondition(boolean replace, ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        ClientPropertyDraw filterProperty = propertyDraw;
        ClientGroupObjectValue filterColumnKey = columnKey;
        
        if(filterProperty == null) {
            filterProperty = logicsSupplier.getSelectedProperty();
            filterColumnKey = logicsSupplier.getSelectedColumn();
        }       
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
        filter.columnKey = filterColumnKey;
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
        if (!logicsSupplier.getFormController().commitCurrentEditing()) {
            return;
        }

        remoteApplyQuery();

        view.queryApplied();
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        if (addNewCondition(true, propertyDraw, columnKey)) {
            setState(State.EXPANDED);
            view.startEditing(initFilterKeyEvent, propertyDraw);
        }
    }

    public boolean hasAnyFilter() {
        return !conditions.isEmpty();
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
