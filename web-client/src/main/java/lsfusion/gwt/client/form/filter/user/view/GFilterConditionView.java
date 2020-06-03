package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.ResizableHorizontalPanel;
import lsfusion.gwt.client.form.filter.user.*;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;

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

    private GFilterConditionListBox propertyView;
    private CheckBox negationView;
    private GFilterConditionListBox compareView;
    private GFilterConditionListBox filterValues;
    private GFilterValueView valueView;
    private GFilterConditionListBox junctionView;

    public GPropertyFilter condition;
    private UIHandler handler;

    private Map<GFilterValue, GFilterValueView> valueViews;

    public GFilterConditionView(GPropertyFilter iCondition, GTableController logicsSupplier, final UIHandler handler) {
        this.condition = iCondition;
        this.handler = handler;
        setVerticalAlignment(ALIGN_MIDDLE);

        propertyView = new GFilterConditionListBox();
        propertyView.addStyleName("customFontPresenter");
        for (GPropertyDraw property : logicsSupplier.getGroupObjectProperties()) {
            propertyView.add(property, property.getNotEmptyCaption());
        }
        propertyView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                condition.property = (GPropertyDraw) propertyView.getSelectedItem();
                condition.columnKey = null;
                filterChanged();
            }
        });
        add(propertyView);

        if (condition.property != null) {
            setSelectedPropertyDraw(condition.property);
        }

        negationView = new CheckBox(messages.formFilterConditionViewNot());
        negationView.addStyleName("checkBoxFilter");
        negationView.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                condition.negation = negationView.getValue();
                handler.conditionChanged();
            }
        });
        negationView.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                handler.conditionChanged();
            }
        });
        add(negationView);
        negationView.setValue(condition.negation);

        compareView = new GFilterConditionListBox();
        compareView.addStyleName("customFontPresenter");
        compareView.add(GCompare.values());
        compareView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                condition.compare = (GCompare) compareView.getSelectedItem();
                handler.conditionChanged();
            }
        });
        add(compareView);
        compareView.setSelectedItem(condition.compare);

        filterValues = new GFilterConditionListBox();
        filterValues.addStyleName("customFontPresenter");

        valueViews = new HashMap<>();

        GDataFilterValue dataValue = condition.value instanceof GDataFilterValue ? (GDataFilterValue) condition.value : new GDataFilterValue();
        GDataFilterValueView dataView = new GDataFilterValueView(this, dataValue, condition.property, logicsSupplier) {
            @Override
            public void applyFilter() {
                handler.applyFilter();
            }
        };
        valueViews.put(dataValue, dataView);

        GObjectFilterValue objectValue = condition.value instanceof GObjectFilterValue ? (GObjectFilterValue) condition.value : new GObjectFilterValue();
        GObjectFilterValueView objectView = new GObjectFilterValueView(this, objectValue, logicsSupplier);
        valueViews.put(objectValue, objectView);

        GPropertyFilterValue propertyValue = condition.value instanceof GPropertyFilterValue ? (GPropertyFilterValue) condition.value : new GPropertyFilterValue();
        GPropertyFilterValueView propertyView = new GPropertyFilterValueView(this, propertyValue, logicsSupplier);
        valueViews.put(propertyValue, propertyView);

        filterValues.add(dataValue, objectValue, propertyValue);
        filterValues.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                condition.value = (GFilterValue) filterValues.getSelectedItem();
                filterChanged();
            }
        });
        add(filterValues);
        filterValues.setSelectedItem(condition.value);

        junctionView = new GFilterConditionListBox();
        junctionView.addStyleName("customFontPresenter");
        junctionView.add(new String[]{messages.formFilterConditionViewAnd(), messages.formFilterConditionViewOr()});
        junctionView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                condition.junction = junctionView.getSelectedIndex() == 0;
                handler.conditionChanged();
            }
        });
        add(junctionView);
        junctionView.setSelectedIndex(condition.junction ? 0 : 1);

        GToolbarButton deleteButton = new GToolbarButton(DELETE, messages.formQueriesFilterRemoveCondition()) {
            @Override
            public void addListener() {
                addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        handler.conditionRemoved(condition);
                    }
                });
            }
        };
        deleteButton.addStyleName("filterDialogButton");
        add(deleteButton);

        valueView = valueViews.get(condition.value);
        if (valueView != null) {
            insert(valueView, getWidgetIndex(junctionView));
            valueView.propertySet(condition);
        }
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
        compareView.setSelectedItem(condition.getDefaultCompare());
        condition.compare = (GCompare) compareView.getSelectedItem();

        handler.conditionChanged();
    }

    public void setJunctionVisible(boolean visible) {
        junctionView.setVisible(visible);
    }

    public void setSelectedPropertyDraw(GPropertyDraw propertyDraw) {
        if (propertyDraw != null) {
            propertyView.setSelectedItem(propertyDraw);
        }
    }

    @Override
    public void valueChanged() {
        handler.conditionChanged();
    }

    public void focusOnValue() {
        valueView.focusOnValue();
    }

    public void startEditing(EditEvent keyEvent) {
        valueView.startEditing(keyEvent);
    }
}
