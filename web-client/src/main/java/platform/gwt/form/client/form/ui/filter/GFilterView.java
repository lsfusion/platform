package platform.gwt.form.client.form.ui.filter;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.base.client.ui.ResizableFocusPanel;
import platform.gwt.base.client.ui.ResizableVerticalPanel;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GPropertyFilter;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form.shared.view.panel.ImageButton;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class GFilterView extends ResizableFocusPanel implements GFilterConditionView.UIHandler {
    private static final String ADD_CONDITION = "filtadd.png";
    private static final String APPLY = "filt.png";

    private ResizableVerticalPanel filterContainer;

    private ImageButton applyButton;

    private GFilterController controller;

    private Map<GPropertyFilter, GFilterConditionView> conditionViews = new LinkedHashMap<GPropertyFilter, GFilterConditionView>();

    public GFilterView(GFilterController iController) {
        controller = iController;

        filterContainer = new ResizableVerticalPanel();
        setWidget(filterContainer);
        addStyleName("noOutline");

        applyButton = new ImageButton(null, APPLY);
        applyButton.addStyleName("toolbarButton");
        applyButton.addStyleName("flowPanelChildLeftAlign");
        applyButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                applyFilter();
            }
        });

        ImageButton addConditionButton = new ImageButton(null, ADD_CONDITION);
        addConditionButton.addStyleName("toolbarButton");
        addConditionButton.addStyleName("flowPanelChildRightAlign");
        addConditionButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.addPressed();
            }
        });

        FlowPanel controlPanel = new FlowPanel();
        controlPanel.add(applyButton);
        controlPanel.add(addConditionButton);

        filterContainer.add(controlPanel);

        sinkEvents(Event.ONKEYDOWN);
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (event.getKeyCode() == KeyCodes.KEY_ESCAPE) {
            GwtClientUtils.stopPropagation(event);
            controller.allRemovedPressed();
        } else {
            super.onBrowserEvent(event);
        }
    }

    public void addCondition(GPropertyFilter condition, GGroupObjectLogicsSupplier logicsSupplier) {
        GFilterConditionView conditionView = new GFilterConditionView(condition, logicsSupplier, this);
        conditionViews.put(condition, conditionView);
        filterContainer.add(conditionView);
        conditionChanged();
        focusOnValue();
    }

    public void removeCondition(GPropertyFilter condition) {
        filterContainer.remove(conditionViews.get(condition));
        conditionViews.remove(condition);
        conditionChanged();
        focusOnValue();
    }

    public void removeAllConditions() {
        for (GPropertyFilter condition : conditionViews.keySet()) {
            filterContainer.remove(conditionViews.get(condition));
        }
        conditionViews.clear();
    }

    @Override
    public void conditionChanged() {
        applyButton.setVisible(true);
        for (GFilterConditionView conditionView : conditionViews.values()) {
            conditionView.setJunctionVisible(Arrays.asList(conditionViews.values().toArray()).indexOf(conditionView) < conditionViews.size() - 1);
        }
    }

    @Override
    public void conditionRemoved(GPropertyFilter condition) {
        controller.removePressed(condition);
    }

    public void queryApplied() {
        applyButton.setVisible(false);
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
        controller.collapsePressed();
        controller.applyPressed();
    }

    public void startEditing(EditEvent keyEvent, GPropertyDraw propertyDraw) {
        if (conditionViews.size() > 0) {
            GFilterConditionView view = conditionViews.values().iterator().next();
            view.setSelectedPropertyDraw(propertyDraw);
            view.startEditing(keyEvent);
        }
    }
}
