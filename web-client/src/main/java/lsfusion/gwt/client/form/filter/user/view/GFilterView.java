package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableFocusPanel;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GUserFilters;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;

import java.util.*;

public class GFilterView extends ResizableFocusPanel implements GFilterConditionView.UIHandler {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String ADD = "filtadd.png";

    private DialogBox filterDialog;

    private ResizableVerticalPanel filterContainer;

    private GUserFilters controller;

    private Map<GPropertyFilter, GFilterConditionView> conditionViews = new LinkedHashMap<>();

    public GFilterView(GUserFilters iController) {
        controller = iController;

        ResizableVerticalPanel mainContainer = new ResizableVerticalPanel();
        setWidget(mainContainer);
        addStyleName("noOutline");

        filterContainer = new ResizableVerticalPanel();

        mainContainer.add(filterContainer);

        Button applyButton = new Button(messages.apply());
        applyButton.addClickHandler(event -> applyFilter());

        Button cancelButton = new Button(messages.cancel());
        cancelButton.addClickHandler(event -> cancelFilter());

        FlowPanel buttonsPanel = new FlowPanel();
        buttonsPanel.add(applyButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.addStyleName("floatRight");
        mainContainer.add(buttonsPanel);

        GToolbarButton addConditionButton = new GToolbarButton(ADD, messages.formQueriesFilterAddCondition() + " (alt + F2)") {
            @Override
            public void addListener() {
                addClickHandler(event -> addNewCondition());
            }
        };
        addConditionButton.addStyleName("flowPanelChildRightAlign");
        addConditionButton.addStyleName("filterDialogButton");

        FlowPanel controlPanel = new FlowPanel();
        controlPanel.add(addConditionButton);
        filterContainer.add(controlPanel);

        sinkEvents(Event.ONKEYDOWN);
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (event.getKeyCode() == KeyCodes.KEY_ESCAPE) {
            GwtClientUtils.stopPropagation(event);
            allRemovedPressed();
        } else {
            super.onBrowserEvent(event);
        }
    }

    public void showDialog(List<GPropertyFilter> conditions, GTableController logicsSupplier, EditEvent keyEvent, GPropertyDraw propertyDraw) {
        if(!conditions.isEmpty()) {
            for (GPropertyFilter condition : conditions) {
                addCondition(condition, logicsSupplier);
            }
            filterDialog = new DialogBox(false, true, new GFilterDialogHeader(messages.formFilterDialogHeader() + " [" + logicsSupplier.getSelectedGroupObject().getCaption() + "]"));
            filterDialog.setWidget(this);
            filterDialog.center();
            focusOnValue();
            if(keyEvent != null) {
                startEditing(keyEvent, propertyDraw);
            }
        }
    }

    public void hideDialog() {
        controller.filterHidden();
        filterDialog.hide();
    }

    public void allRemovedPressed() {
        controller.allRemovedPressed();
        hideDialog();
    }

    public void addNewCondition() {
        addCondition(controller.getNewCondition(null, null), controller.getLogicsSupplier());

    }

    public void addCondition(GPropertyFilter condition, GTableController logicsSupplier) {
        if(condition != null) {
            GFilterConditionView conditionView = new GFilterConditionView(condition, logicsSupplier, this);
            conditionViews.put(condition, conditionView);
            filterContainer.add(conditionView);
            conditionChanged();
            focusOnValue();
        }
    }

    public void removeCondition(GPropertyFilter condition) {
        filterContainer.remove(conditionViews.get(condition));
        conditionViews.remove(condition);
        conditionChanged();
        focusOnValue();
    }

    @Override
    public void conditionChanged() {
        for (GFilterConditionView conditionView : conditionViews.values()) {
            conditionView.setJunctionVisible(Arrays.asList(conditionViews.values().toArray()).indexOf(conditionView) < conditionViews.size() - 1);
        }
    }

    @Override
    public void conditionRemoved(GPropertyFilter condition) {
        removeCondition(condition);
        if (conditionViews.isEmpty()) {
            allRemovedPressed();
        }
    }

    public void focusOnValue() {
        if (!conditionViews.isEmpty()) {
            // пробегаем по всем ячейкам со значеними, останавливаясь на последней, чтобы сбросить стили выделения в остальных
            for (GFilterConditionView filterView : conditionViews.values()) {
                filterView.focusOnValue();
            }
        }
    }

    public void applyFilter() {
        controller.applyFilters(new ArrayList<>(conditionViews.keySet()), true);
        hideDialog();
    }

    public void cancelFilter() {
        hideDialog();
    }

    public void startEditing(EditEvent keyEvent, GPropertyDraw propertyDraw) {
        if (conditionViews.size() > 0) {
            GFilterConditionView view = conditionViews.values().iterator().next();
            view.setSelectedPropertyDraw(propertyDraw);
            view.startEditing(keyEvent);
        }
    }
}
