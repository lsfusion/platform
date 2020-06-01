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
    private static final String FILTER_BUTTON_TOOLTIP_TEXT = messages.formQueriesFilter() + " (F2)";

    private GToolbarButton toolbarButton;
    private GFilterView filterView;
    private DialogBox filterDialog;
    private GFilterDialogHeader filterDialogHeader;

    private List<GPropertyFilter> conditions = new ArrayList<>();

    private boolean expanded = false;

    private GTableController logicsSupplier;

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
        updateToolbarButton();

        filterView = new GFilterView(this);

        filterDialogHeader = new GFilterDialogHeader(messages.formFilterDialogHeader());
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
        if (visible) {
            getFilterDialog().show();
        } else if (filterDialog != null) {
            getFilterDialog().hide();
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
        if (hasConditions() || addNewCondition(false)) {
            changeState(true);
        }
    }

    private void changeState(boolean expand) {
        if (expand) {
            for (GPropertyFilter condition : conditions) {
                filterView.addCondition(condition, logicsSupplier, true);
            }
            filterDialogHeader.setText(messages.formFilterDialogHeader() + " [" + logicsSupplier.getSelectedGroupObject().getCaption() + "]");
            getFilterDialog().center();
            focusOnValue();
        } else {
            updateToolbarButton();
            filterHidden();
        }
        toolbarButton.setEnabled(!expand);
        setDialogVisible(expand);
        this.expanded = expand;
    }

    public void updateToolbarButton() {
        boolean hasConditions = hasConditions();
        toolbarButton.setTitle(hasConditions ? messages.expandFilterWindow() : FILTER_BUTTON_TOOLTIP_TEXT);
        toolbarButton.showBackground(hasConditions);
    }

    public void collapsePressed() {
        filterView.removeAllConditions();
        changeState(false);
    }

    public void addPressed() {
        if (addNewCondition(false)) {
            changeState(true);
        }
    }

    public void replaceConditionPressed() {
        if (addNewCondition(true)) {
            changeState(true);
        }
    }

    public boolean addNewCondition(boolean replace) {
        return addNewCondition(replace, null, null);
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
        filterView.addCondition(filter, logicsSupplier, false);
        return true;
    }

    public void addNewConditions(List<GPropertyFilter> filters) {
        for(GPropertyFilter filter : filters) {
            GPropertyFilter condition = findConditionByProperty(filter.property);
            if(condition != null) {
                conditions.remove(condition);
            }
            conditions.add(filter);
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
        filterView.removeCondition(filter);

        if (filterView.isEmpty()) {
            allRemovedPressed();
        }
    }

    public void removeAllConditions() {
        conditions.clear();
        filterView.removeAllConditions();
    }

    public void allRemovedPressed() {
        removeAllConditions();
        applyQuery();
        changeState(false);
    }

    public void applyFilter(List<GPropertyFilter> conditions) {
        this.conditions = conditions;
        collapsePressed();
        applyPressed();
    }

    public void applyPressed() {
        applyQuery();
    }

    public void applyQuery() {
        remoteApplyQuery();
    }

    public void focusOnValue() {
        filterView.focusOnValue();
    }

    public void setVisible(boolean visible) {
        setDialogVisible(visible && expanded);
    }

    public void quickEditFilter(EditEvent keyEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        if (addNewCondition(true, propertyDraw, columnKey)) {
            changeState(true);
            filterView.startEditing(keyEvent, propertyDraw);
        }
    }

    public abstract void remoteApplyQuery();
    public abstract void filterHidden();
}
