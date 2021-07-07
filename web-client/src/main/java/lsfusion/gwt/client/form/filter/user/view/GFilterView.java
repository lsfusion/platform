package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GUserFilters;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.form.event.GKeyStroke.ADD_USER_FILTER_KEY_STROKE;

public class GFilterView extends FlexPanel implements GFilterConditionView.UIHandler {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String ADD_ICON_PATH = "filtadd.png";
    private static final String RESET_ICON_PATH = "filtreset.png";

    private FlexPanel filterContainer;

    private GToolbarButton addConditionButton;
    private GToolbarButton resetConditionsButton;

    private GUserFilters controller;
    private GFilter filterComponent;
    private boolean initialized = false;

    private Map<GPropertyFilter, GFilterConditionView> conditionViews = new LinkedHashMap<>();

    private boolean toolsVisible = false;

    public GFilterView(GUserFilters iController, GFilter filter) {
        controller = iController;
        filterComponent = filter;

        FlexPanel mainContainer = new FlexPanel();
        addFill(mainContainer, GFlexAlignment.START);
        addStyleName("userFiltersPanel");

        filterContainer = new FlexPanel();

        mainContainer.addFill(filterContainer, GFlexAlignment.START);

        FlexPanel buttonsPanel = new FlexPanel(); 

        addConditionButton = new GToolbarButton(ADD_ICON_PATH, messages.formQueriesFilterAddCondition()) {
            @Override
            public ClickHandler getClickHandler() {
                // pass add filter key down event to start editing immediately
                return event -> addCondition(Event.as(Document.get().createKeyDownEvent(ADD_USER_FILTER_KEY_STROKE.ctrlPressed,
                        ADD_USER_FILTER_KEY_STROKE.altPressed,
                        ADD_USER_FILTER_KEY_STROKE.shiftPressed,
                        false,
                        ADD_USER_FILTER_KEY_STROKE.keyCode)));
            }
        };
        addConditionButton.addStyleName("userFilterButton");
        addConditionButton.setVisible(toolsVisible);
        buttonsPanel.add(addConditionButton);

        resetConditionsButton = new GToolbarButton(RESET_ICON_PATH, messages.formQueriesFilterResetConditions()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> allRemovedPressed();
            }
        };
        resetConditionsButton.addStyleName("userFilterButton");
        resetConditionsButton.setVisible(toolsVisible);
        buttonsPanel.add(resetConditionsButton);

        mainContainer.add(buttonsPanel);
    }

    // similar to GFormController.processBinding
    private void processBinding(NativeEvent event, Runnable action) {
        controller.checkCommitEditing();
        action.run();
        stopPropagation(event);
    }
    
    public void allRemovedPressed() {
        filterContainer.clear();
        conditionViews.clear();
        controller.allRemoved();
    }

    public void addCondition(Event keyEvent) {
        addCondition(keyEvent, false);
    }

    public void addCondition(Event keyEvent, boolean replace) {
        addCondition(null, null, keyEvent, replace);
    }
    
    public void addCondition(GPropertyDraw property, GGroupObjectValue columnKey) {
        addCondition(property, columnKey, null, false);
    }

    public void addCondition(GPropertyDraw property, GGroupObjectValue columnKey, Event keyEvent, boolean replace) {
        addCondition(controller.getNewCondition(property, columnKey), keyEvent, replace);
    }

    public void addCondition(GPropertyFilter condition, Event keyEvent, boolean replace) {
        if (replace) {
            allRemovedPressed();
        }
        if (condition != null) {
            GFilterConditionView conditionView = new GFilterConditionView(condition, controller.getLogicsSupplier(), this, toolsVisible);
            conditionViews.put(condition, conditionView);
            filterContainer.add(conditionView);
            
            updateConditionsLastState();
            focusLastValue();

            if (keyEvent != null) {
                conditionView.startEditing(keyEvent);
            }
        }
    }

    @Override
    public void conditionRemoved(GPropertyFilter condition) {
        GFilterConditionView view = conditionViews.get(condition);
        
        GFilterConditionView nextViewToFocus = null;
        if (conditionViews.size() > 1 && view.isFocused()) {
            ArrayList<GFilterConditionView> viewsList = new ArrayList<>(conditionViews.values());
            int currentIndex = viewsList.indexOf(view);
            nextViewToFocus = viewsList.get(currentIndex == viewsList.size() - 1 ? currentIndex - 1 : currentIndex + 1);
        }
        
        conditionViews.remove(condition);
        filterContainer.remove(view);
        
        updateConditionsLastState();
        if (nextViewToFocus != null) {
            nextViewToFocus.focusOnValue();
        }
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
        resetConditionsButton.setVisible(toolsVisible);
    }

    @Override
    public void addEnterBinding(Widget widget) {
        controller.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER)),
                GBindingEnv.AUTO,
                event -> GFilterView.this.processBinding(event, GFilterView.this::applyFilters),
                widget);
    }

    private void updateConditionsLastState() {
        int i = 0;
        for (GFilterConditionView cView : conditionViews.values()) {
            i++;
            cView.setLast(i == conditionViews.size());
        }
    }

    public void focusLastValue() {
        if (!conditionViews.isEmpty()) {
            Object[] views = conditionViews.values().toArray();
            ((GFilterConditionView) views[views.length - 1]).focusOnValue();
        }
    }

    public void applyFilters() {
        ArrayList<GPropertyFilter> result = new ArrayList<>();
        for (Map.Entry<GPropertyFilter, GFilterConditionView> entry : conditionViews.entrySet()) {
            if (entry.getValue().allowNull || !entry.getKey().nullValue()) {
                result.add(entry.getKey());
            }
        }
        controller.applyFilters(result, true);
    }

    public boolean hasConditions() {
        return !conditionViews.isEmpty();
    }
    
    public void update() {
        if (!initialized) {
            for (GPropertyDraw property : filterComponent.properties) {
                addCondition(property, controller.getLogicsSupplier().getSelectedColumnKey());
            }
            initialized = true;
        }
    }
}
