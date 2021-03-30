package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GUserFilters;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class GFilterView extends FlexPanel implements GFilterConditionView.UIHandler {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String ADD_ICON_PATH = "filtadd.png";
    private static final String APPLY_ICON_PATH = "filtapply.png";
    private static final String RESET_ICON_PATH = "filtreset.png";

    private FlexPanel filterContainer;

    private GToolbarButton addConditionButton;
    private GToolbarButton applyButton;
    private GToolbarButton resetConditionsButton;
    private Widget buttonsReplacement;

    private GUserFilters controller;

    private Map<GPropertyFilter, GFilterConditionView> conditionViews = new LinkedHashMap<>();

    private boolean toolsVisible = false;

    public GFilterView(GUserFilters iController, GFilter filter) {
        controller = iController;

        FlexPanel mainContainer = new FlexPanel();
        add(mainContainer);
        setChildFlex(mainContainer, 1);
        addStyleName("userFiltersPanel");

        filterContainer = new FlexPanel();

        mainContainer.add(filterContainer);
        mainContainer.setChildFlex(filterContainer, 1);
        
        FlexPanel buttonsPanel = new FlexPanel(); 

        addConditionButton = new GToolbarButton(ADD_ICON_PATH, messages.formQueriesFilterAddCondition()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> addCondition();
            }
        };
        addConditionButton.addStyleName("userFilterButton");
        addConditionButton.setVisible(toolsVisible);
        buttonsPanel.add(addConditionButton);

        applyButton = new GToolbarButton(APPLY_ICON_PATH, messages.formQueriesFilterApply()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> applyFilter();
            }
        };
        applyButton.addStyleName("userFilterButton");
        applyButton.setVisible(toolsVisible);
        buttonsPanel.add(applyButton);

        resetConditionsButton = new GToolbarButton(RESET_ICON_PATH, messages.formQueriesFilterResetConditions()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> allRemovedPressed();
            }
        };
        resetConditionsButton.addStyleName("userFilterButton");
        resetConditionsButton.setVisible(toolsVisible);
        buttonsPanel.add(resetConditionsButton);

        buttonsReplacement = GwtClientUtils.createHorizontalStrut((StyleDefaults.COMPONENT_HEIGHT + 4) * 3); // to prevent container from changing size on showing tools, 4 - margin
        buttonsReplacement.setVisible(!toolsVisible);
        buttonsPanel.add(buttonsReplacement);
        
        mainContainer.add(buttonsPanel);

        // todo: not working with two grids (applyQuery() with empty conditions?) / EDIT 
        controller.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER)), new GBindingEnv(100, null, null, null, null, null, null, null), event -> processBinding(event, this::applyFilter), this);
//        controller.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ESCAPE)), GBindingEnv.AUTO, event -> processBinding(event, this::allRemovedPressed), this);

        for (GPropertyDraw property : filter.properties) {
            addCondition(property);
        }
    }

    // similar to GFormController.processBinding
    private void processBinding(NativeEvent event, Runnable action) {
        controller.checkCommitEditing();
        action.run();
        stopPropagation(event);
    }
    
    private void clearConditions() {
        for (GFilterConditionView filter : conditionViews.values()) {
            filterContainer.remove(filter);
        }
        conditionViews.clear();
    }

    public void allRemovedPressed() {
        clearConditions();
        controller.allRemoved();
    }

    public void addCondition() {
        addCondition(null, null, null, false);
    }

    public void addCondition(boolean replace) {
        addCondition(null, null, null, replace);
    }
    
    public void addCondition(GPropertyDraw property) {
        addCondition(property, null, null, false);
    }

    public void addCondition(GPropertyDraw property, GGroupObjectValue columnKey, Event keyEvent, boolean replace) {
        addCondition(controller.getNewCondition(property, columnKey), keyEvent, replace);
    }

    public void addCondition(GPropertyFilter condition, Event keyEvent, boolean replace) {
        if (replace) {
            clearConditions();
        }
        if (condition != null) {
            GFilterConditionView conditionView = new GFilterConditionView(condition, controller.getLogicsSupplier(), this, toolsVisible);
            conditionViews.put(condition, conditionView);
            filterContainer.add(conditionView);
            conditionChanged();
            focusLastValue();

            if (keyEvent != null) {
                conditionView.startEditing(keyEvent);
            }
        }
    }

    public void removeCondition(GPropertyFilter condition) {
        filterContainer.remove(conditionViews.get(condition));
        conditionViews.remove(condition);
        conditionChanged();
        focusLastValue();
    }
    
    public boolean isToolsVisible() {
        return toolsVisible;
    }

    public void toggleToolsVisible() {
        toolsVisible = !toolsVisible;
        for (GFilterConditionView view : conditionViews.values()) {
            view.setToolsVisible(toolsVisible);
        }
        
        addConditionButton.setVisible(toolsVisible);
        applyButton.setVisible(toolsVisible);
        resetConditionsButton.setVisible(toolsVisible);
        
        buttonsReplacement.setVisible(!toolsVisible);
    }

    @Override
    public void conditionChanged() {
        int i = 0;
        for (GFilterConditionView cView : conditionViews.values()) {
            i++;
            cView.setJunctionVisible(i < conditionViews.size());
        }
    }

    @Override
    public void conditionRemoved(GPropertyFilter condition) {
        removeCondition(condition);
    }

    public void focusLastValue() {
        if (!conditionViews.isEmpty()) {
            // пробегаем по всем ячейкам со значеними, останавливаясь на последней, чтобы сбросить стили выделения в остальных
//            for (GFilterConditionView filterView : conditionViews.values()) {
//                filterView.focusOnValue();
//            }
            Object[] views = conditionViews.values().toArray();
            ((GFilterConditionView) views[views.length - 1]).focusOnValue();
        }
    }

    public void applyFilter() {
//        if (!conditionViews.isEmpty()) {
            controller.applyFilters(new ArrayList<>(conditionViews.keySet()), true);
//        }
    }

    public boolean hasConditions() {
        return !conditionViews.isEmpty();
    }
}
