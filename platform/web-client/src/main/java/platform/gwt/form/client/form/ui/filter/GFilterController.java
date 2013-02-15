package platform.gwt.form.client.form.ui.filter;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import platform.gwt.form.client.HotkeyManager;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GPropertyFilter;
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
        filterDialog = new DialogBox(false, false, filterDialogHeader);
        filterDialog.setWidget(filterView);

    }

    public Button getToolbarButton() {
        return toolbarButton;
    }

    public List<GPropertyFilter> getConditions() {
        return conditions;
    }

    private void showFilterPressed() {
        if (state == State.REMOVED) {
            addPressed();
        }
        changeState(State.EXPANDED);
    }

    private void changeState(State newState) {
        filterDialog.setVisible(newState == State.EXPANDED);
        if (newState == State.EXPANDED && state == State.REMOVED) {
            filterDialogHeader.setText("Фильтр [" + logicsSupplier.getSelectedGroupObject().getCaption() + "]");
            filterDialog.center();
        }

        state = newState;

        String toolbarButtonIconPath = null;
        switch (state) {
            case REMOVED:
                toolbarButtonIconPath = ADD_FILTER;
                break;
            case COLLAPSED:
                toolbarButtonIconPath = EXPAND;
        }
        if (toolbarButtonIconPath != null) {
            toolbarButton.setModuleImagePath(toolbarButtonIconPath);
        }
        toolbarButton.setEnabled(state != State.EXPANDED);

        if (state != State.EXPANDED) {
            filterHidden();
        } else {
            focusOnValue();
        }
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
        filterDialog.setVisible(visible && state != State.COLLAPSED && state != State.REMOVED);
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

    public void addHotKeys(Element element) {
        HotkeyManager.get().addHotkeyBinding(element, new GKeyStroke(GKeyStroke.KEY_F2), new HotkeyManager.Binding() {
            @Override
            public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                replaceConditionPressed();
                return true;
            }
        });

        HotkeyManager.get().addHotkeyBinding(element, new GKeyStroke(GKeyStroke.KEY_F2, true, false, false), new HotkeyManager.Binding() {
            @Override
            public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                addPressed();
                return true;
            }
        });

        HotkeyManager.get().addHotkeyBinding(element, new GKeyStroke(GKeyStroke.KEY_F2, false, false, true), new HotkeyManager.Binding() {
            @Override
            public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                allRemovedPressed();
                return true;
            }
        });
    }

    public abstract void remoteApplyQuery();
    public abstract void filterHidden();
}
