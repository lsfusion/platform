package platform.gwt.form.client.form.ui.filter;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GPropertyFilter;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form.shared.view.panel.ImageButton;

import java.util.ArrayList;
import java.util.List;

public abstract class GFilterController {
    private static final String ADD_FILTER = "filtadd.png";
    private static final String EXPAND = "expand.png";

    private ImageButton toolbarButton;
    private GFilterView filterView;
    private DialogBox filterDialog;
    private GFilterDialogHeader filterDialogHeader;

    private List<GPropertyFilter> conditions = new ArrayList<GPropertyFilter>();

    private State state = State.REMOVED;
    private State hiddenState;

    private GGroupObjectLogicsSupplier logicsSupplier;

    private enum State {
        HIDDEN, REMOVED, COLLAPSED, EXPANDED
    }

    public GFilterController(GGroupObjectLogicsSupplier logicsSupplier) {
        this.logicsSupplier = logicsSupplier;

        toolbarButton = new ImageButton(null, ADD_FILTER);
        toolbarButton.addStyleName("toolbarButton");
        toolbarButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showFilterPressed();
            }
        });

        filterView = new GFilterView(this);

        filterDialogHeader = new GFilterDialogHeader("Фильтр") {
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
            filterDialogHeader.setText("Фильтр [" + logicsSupplier.getSelectedGroupObject().getCaption() + "]");
            getFilterDialog().center();
        }

        String toolbarButtonIconPath = null;
        switch (newState) {
            case REMOVED:
                toolbarButtonIconPath = ADD_FILTER;
                toolbarButton.getElement().getStyle().setProperty("background", "");
                break;
            case COLLAPSED:
                toolbarButtonIconPath = EXPAND;
                toolbarButton.getElement().getStyle().setProperty("background", "#A2FFA2");
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

    public void collapsePressed() {
        changeState(State.COLLAPSED);
    }

    public void addPressed() {
        if (addNewCondition(false, null)) {
            changeState(State.EXPANDED);
        }
    }

    public void replaceConditionPressed() {
        if (addNewCondition(true, null)) {
            changeState(State.EXPANDED);
        }
    }

    private boolean addNewCondition(boolean replace, GPropertyDraw property) {
        GPropertyDraw filterProperty = property != null ? property : logicsSupplier.getSelectedProperty();
        if (filterProperty == null) {
            return false;
        }

        if (replace) {
            removeAllConditions();
        }

        GPropertyFilter filter = new GPropertyFilter();
        filter.property = filterProperty;
        filter.groupObject = logicsSupplier.getSelectedGroupObject();
        conditions.add(filter);
        filterView.addCondition(filter, logicsSupplier);
        return true;
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

    public void quickEditFilter(EditEvent keyEvent, GPropertyDraw propertyDraw) {
        if (addNewCondition(true, propertyDraw)) {
            changeState(State.EXPANDED);
            filterView.startEditing(keyEvent, propertyDraw);
        }
    }

    public abstract void remoteApplyQuery();
    public abstract void filterHidden();
}
