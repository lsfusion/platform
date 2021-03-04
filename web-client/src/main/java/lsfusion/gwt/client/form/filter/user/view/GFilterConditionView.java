package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.ResizableHorizontalPanel;
import lsfusion.gwt.client.form.filter.user.*;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;

import java.util.HashMap;
import java.util.Map;

public class GFilterConditionView extends ResizableHorizontalPanel implements GFilterValueView.GFilterValueListener {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    public interface UIHandler {
        void conditionChanged();
        void conditionRemoved(GPropertyFilter condition);
        void applyFilter();
    }

    private static final String DELETE = "filtdel.png";

    private Label propertyLabel;
    private CheckBox negationView;
    private GFilterConditionListBox compareView;
    private GFilterConditionListBox filterValues;
    private GFilterValueView valueView;
    private GFilterConditionListBox junctionView;
    private GToolbarButton deleteButton;
    
    private boolean toolsVisible;
    private boolean junctionVisible = false;

    public GPropertyFilter condition;
    private UIHandler handler;

    private Map<GFilterValue, GFilterValueView> valueViews;

    public GFilterConditionView(GPropertyFilter iCondition, GTableController logicsSupplier, final UIHandler handler, boolean toolsVisible) {
        this.condition = iCondition;
        this.handler = handler;
        setVerticalAlignment(ALIGN_MIDDLE);

        propertyLabel = new Label(condition.property.getNotEmptyCaption());
        add(propertyLabel);

        negationView = new CheckBox(messages.formFilterConditionViewNot());
        negationView.addStyleName("checkBoxFilter");
        negationView.addValueChangeHandler(event -> {
            condition.negation = negationView.getValue();
            handler.conditionChanged();
        });
        negationView.addClickHandler(event -> handler.conditionChanged());
        add(negationView);
        negationView.setValue(condition.negation);

        compareView = new GFilterConditionListBox();
        compareView.addStyleName("customFontPresenter");
        compareView.add((Object[]) GCompare.values());
        compareView.addChangeHandler(event -> {
            condition.compare = (GCompare) compareView.getSelectedItem();
            handler.conditionChanged();
        });
        add(compareView);
        compareView.setItems(condition.property.baseType.getFilterCompares());
        compareView.setSelectedItem(condition.compare);

        filterValues = new GFilterConditionListBox();
        filterValues.addStyleName("customFontPresenter");

        valueViews = new HashMap<>();

        GDataFilterValue dataValue = condition.value instanceof GDataFilterValue ? (GDataFilterValue) condition.value : new GDataFilterValue();
        GDataFilterValueView dataView = new GDataFilterValueView(this, dataValue, condition.property, logicsSupplier);
        valueViews.put(dataValue, dataView);

        GObjectFilterValue objectValue = condition.value instanceof GObjectFilterValue ? (GObjectFilterValue) condition.value : new GObjectFilterValue();
        GObjectFilterValueView objectView = new GObjectFilterValueView(this, objectValue, logicsSupplier);
        valueViews.put(objectValue, objectView);

        GPropertyFilterValue propertyValue = condition.value instanceof GPropertyFilterValue ? (GPropertyFilterValue) condition.value : new GPropertyFilterValue();
        GPropertyFilterValueView propertyView = new GPropertyFilterValueView(this, propertyValue, logicsSupplier);
        valueViews.put(propertyValue, propertyView);

        filterValues.add(dataValue, objectValue, propertyValue);
        filterValues.addChangeHandler(event -> {
            condition.value = (GFilterValue) filterValues.getSelectedItem();
            filterChanged();
        });
        add(filterValues);
        filterValues.setSelectedItem(condition.value);

        junctionView = new GFilterConditionListBox();
        junctionView.addStyleName("customFontPresenter");
        junctionView.add(new Object[]{messages.formFilterConditionViewAnd(), messages.formFilterConditionViewOr()});
        junctionView.addChangeHandler(event -> {
            condition.junction = junctionView.getSelectedIndex() == 0;
            handler.conditionChanged();
        });
        add(junctionView);
        junctionView.setSelectedIndex(condition.junction ? 0 : 1);

        deleteButton = new GToolbarButton(DELETE, messages.formQueriesFilterRemoveCondition()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> handler.conditionRemoved(condition);
            }
        };
        deleteButton.addStyleName("filterDialogButton");
        add(deleteButton);

        valueView = valueViews.get(condition.value);
        if (valueView != null) {
            insert(valueView, getWidgetIndex(junctionView));
            valueView.propertySet(condition);
        }
        
        setToolsVisible(toolsVisible);
    }

    private void filterChanged() {
        if (valueView != null) {
            remove(valueView);
        }
        valueView = valueViews.get(condition.value);
        if (valueView != null) {
            insert(valueView, getWidgetIndex(junctionView));
            valueView.propertyChanged(condition);
        }
        compareView.setItems(condition.property.baseType.getFilterCompares());
        compareView.setSelectedItem(condition.property.getDefaultCompare());
        condition.compare = (GCompare) compareView.getSelectedItem();

        handler.conditionChanged();
    }

    public void setJunctionVisible(boolean visible) {
        junctionVisible = visible;
        junctionView.setVisible(visible && toolsVisible);
    }

    public void setToolsVisible(boolean visible) {
        toolsVisible = visible;
        negationView.setVisible(visible);
        compareView.setVisible(visible);
        filterValues.setVisible(visible);
        junctionView.setVisible(junctionVisible && visible);
        deleteButton.setVisible(visible);
    }

    @Override
    public void valueChanged() {
        handler.conditionChanged();
    }

    public void focusOnValue() {
        valueView.focusOnValue();
    }

    public void startEditing(Event keyEvent) {
        valueView.startEditing(keyEvent);
    }
}
