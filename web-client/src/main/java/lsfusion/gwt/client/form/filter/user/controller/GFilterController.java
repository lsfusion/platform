package lsfusion.gwt.client.form.filter.user.controller;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.view.GFilterDialogHeader;
import lsfusion.gwt.client.form.filter.user.view.GFilterView;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class GFilterController {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String FILTER = "filt.png";
    private static final String COLLAPSE = "collapse.png";
    private static final String FILTER_BUTTON_TOOLTIP_TEXT = messages.formQueriesFilter() + " (F2)";

    private GToolbarButton toolbarButton;
    private GFilterView filterView;
    private DialogBox filterDialog;
    private GFilterDialogHeader filterDialogHeader;

    private List<GPropertyFilter> conditions = new ArrayList<>();

    private State state = State.REMOVED;
    private State hiddenState;

    private GTableController logicsSupplier;

    private enum State {
        HIDDEN, REMOVED, COLLAPSED, EXPANDED
    }

    public GFilterController(GTableController logicsSupplier) {
        this.logicsSupplier = logicsSupplier;

        toolbarButton = new GToolbarButton(FILTER) {
            @Override
            public void addListener() {
                addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        showFilterPressed();
                    }
                });
            }
        };
        toolbarButton.setTitle(FILTER_BUTTON_TOOLTIP_TEXT);

        filterView = new GFilterView(this);

        filterDialogHeader = new GFilterDialogHeader(messages.formFilterDialogHeader()) {
            @Override
            public void collapseButtonPressed() {
                collapsePressed();
            }
        };
//        filterDialog = new DialogBox(false, false, filterDialogHeader);
//        filterDialog.setWidget(filterView);
    }

    public DialogBox getFilterDialog() {
        // DialogBox в конструкторе запрашивает getClientWidth, что вызывает relayout
        // поэтому не конструируем его, пока не понадобится, чтобы не тормозить начальный показ формы
        if (filterDialog == null) {
            filterDialog = new DialogBox(false, false, filterDialogHeader);
            filterDialog.setWidget(filterView);

        }
        return filterDialog;
    }

    public boolean isDialogAttached() {
        return filterDialog != null && filterDialog.isAttached();
    }

    public void reattachDialog() {
        if (isDialogAttached()) {
            getFilterDialog().hide();
            getFilterDialog().show();
        }
    }

    private void setDialogVisible(boolean visible) {
        // игнорируем setVisible(false), пока диалог не создан
        if (visible || filterDialog != null) {
            getFilterDialog().setVisible(visible);
        }
    }

    public Button getToolbarButton() {
        return toolbarButton;
    }

    public List<GPropertyFilter> getConditions() {
        return conditions;
    }

    public boolean hasConditions() {
        return !conditions.isEmpty();
    }

    private void showFilterPressed() {
        if (state == State.REMOVED) {
            addPressed();
        }
        changeState(State.EXPANDED);
    }

    private void changeState(State newState) {
        setDialogVisible(newState == State.EXPANDED);
        if (newState == State.EXPANDED && state == State.REMOVED) {
            filterDialogHeader.setText(messages.formFilterDialogHeader() + " [" + logicsSupplier.getSelectedGroupObject().getCaption() + "]");
            getFilterDialog().center();
        }

        String toolbarButtonIconPath = null;
        switch (newState) {
            case REMOVED:
                toolbarButtonIconPath = FILTER;
                toolbarButton.setTitle(FILTER_BUTTON_TOOLTIP_TEXT);
                if (state != State.HIDDEN) {
                    toolbarButton.showBackground(false);
                }
                break;
            case COLLAPSED:
                toolbarButtonIconPath = COLLAPSE;
                toolbarButton.setTitle(messages.expandFilterWindow());
                if (state != State.HIDDEN) {
                    toolbarButton.showBackground(true);
                }
        }
        if (toolbarButtonIconPath != null) {
            toolbarButton.setModuleImagePath(toolbarButtonIconPath);
        }
        toolbarButton.setEnabled(newState != State.EXPANDED);

        if (newState != State.EXPANDED) {
            if (state == State.EXPANDED) {
                filterHidden();
            }
        } else {
            focusOnValue();
        }

        state = newState;
    }

    public void expandPressed() {
        changeState(State.EXPANDED);
    }

    public void collapsePressed() {
        changeState(State.COLLAPSED);
    }

    public void addPressed() {
        if (addNewCondition(false, null, null)) {
            changeState(State.EXPANDED);
        }
    }

    public void replaceConditionPressed() {
        if (addNewCondition(true, null, null)) {
            changeState(State.EXPANDED);
        }
    }

    private boolean addNewCondition(boolean replace, GPropertyDraw property, GGroupObjectValue columnKey) {
        GPropertyDraw filterProperty = property;
        GGroupObjectValue filterColumnKey = columnKey;

        if(filterProperty == null) {
            filterProperty = logicsSupplier.getSelectedProperty();
            if(filterProperty != null)
                filterColumnKey = logicsSupplier.getSelectedColumn();
        }
        if (filterProperty == null) {
            //не добавляем, если нет ни одного свойства
            return false;
        }

        if (replace) {
            removeAllConditions();
        }

        GPropertyFilter filter = new GPropertyFilter();
        filter.property = filterProperty;
        filter.columnKey = filterColumnKey;
        filter.groupObject = logicsSupplier.getSelectedGroupObject();
        conditions.add(filter);
        filterView.addCondition(filter, logicsSupplier);
        return true;
    }

    public void addNewConditions(List<GPropertyFilter> filters) {
        for(GPropertyFilter filter : filters) {
            GPropertyFilter condition = findConditionByProperty(filter.property);
            if(condition != null) {
                conditions.remove(condition);
                filterView.removeCondition(condition);
            }
            conditions.add(filter);
            filterView.addCondition(filter, logicsSupplier);
        }
    }

    public GPropertyFilter findConditionByProperty(GPropertyDraw property) {
        for(GPropertyFilter condition : conditions) {
            if(condition.property.equals(property)) {
                return condition;
            }
        }
        return null;
    }

    public void removePressed(GPropertyFilter filter) {
        conditions.remove(filter);
        filterView.removeCondition(filter);

        if (conditions.isEmpty()) {
            applyQuery();
            changeState(State.REMOVED);
        }
    }

    public void removeAllConditions() {
        conditions.clear();
        filterView.removeAllConditions();
    }

    public void allRemovedPressed() {
        removeAllConditions();
        applyQuery();
        changeState(State.REMOVED);
    }

    public void applyPressed() {
        applyQuery();
    }

    public void applyQuery() {
        remoteApplyQuery();
        filterView.queryApplied();
    }

    public void focusOnValue() {
        filterView.focusOnValue();
    }

    public void setVisible(boolean visible) {
        setDialogVisible(visible && state != State.COLLAPSED && state != State.REMOVED);
        if (!visible) {
            if (state != State.HIDDEN) {
                hiddenState = state;
                changeState(State.HIDDEN);
            }
        } else {
            if (state == State.HIDDEN) {
                changeState(hiddenState);
            }
        }
    }

    public void quickEditFilter(EditEvent keyEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        if (addNewCondition(true, propertyDraw, columnKey)) {
            changeState(State.EXPANDED);
            filterView.startEditing(keyEvent, propertyDraw);
        }
    }

    public abstract void remoteApplyQuery();
    public abstract void filterHidden();
}
