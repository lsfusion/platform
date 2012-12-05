package platform.gwt.form.client.form.ui.filter;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.*;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form.shared.view.panel.ImageButton;

import java.util.HashMap;
import java.util.Map;

public class GFilterConditionView extends HorizontalPanel implements GFilterValueView.GFilterValueListener {
    public interface UIHandler {
        void conditionChanged();
        void conditionRemoved(GPropertyFilter condition);
        void applyFilter();
    }

    private static final String DELETE = "delete.png";

    private GFilterConditionListBox propertyView;
    private CheckBox negationView;
    private GFilterConditionListBox compareView;
    private GFilterConditionListBox filterValues;
    private GFilterValueView valueView;
    private GFilterConditionListBox junctionView;
    private ImageButton deleteButton;

    public GPropertyFilter condition;
    private UIHandler handler;

    private Map<GFilterValue, GFilterValueView> valueViews;

    public GFilterConditionView(GPropertyFilter iCondition, GGroupObjectLogicsSupplier logicsSupplier, final UIHandler handler) {
        this.condition = iCondition;
        this.handler = handler;

        propertyView = new GFilterConditionListBox();
        propertyView.addStyleName("customFontPresenter");
        for (GPropertyDraw property : logicsSupplier.getGroupObjectProperties()) {
            propertyView.add(property, property.getNotEmptyCaption());
        }
        propertyView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                condition.property = (GPropertyDraw) propertyView.getSelectedValue();
                filterChanged();
            }
        });
        add(propertyView);

        if (condition.property != null) {
            setSelectedPropertyDraw(condition.property);
        }

        negationView = new CheckBox("НЕ");
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

        compareView = new GFilterConditionListBox();
        compareView.addStyleName("customFontPresenter");
        compareView.add(GCompare.values());
        compareView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                condition.compare = (GCompare) compareView.getSelectedValue();
                handler.conditionChanged();
            }
        });
        add(compareView);

        filterValues = new GFilterConditionListBox();
        filterValues.addStyleName("customFontPresenter");

        valueViews = new HashMap<GFilterValue, GFilterValueView>();

        GDataFilterValue dataValue = new GDataFilterValue();
        GDataFilterValueView dataView = new GDataFilterValueView(this, dataValue, condition.property, logicsSupplier) {
            @Override
            public void applyFilter() {
                handler.applyFilter();
            }
        };
        valueViews.put(dataValue, dataView);

        GObjectFilterValue objectValue = new GObjectFilterValue();
        GObjectFilterValueView objectView = new GObjectFilterValueView(this, objectValue, logicsSupplier);
        valueViews.put(objectValue, objectView);

        GPropertyFilterValue propertyValue = new GPropertyFilterValue();
        GPropertyFilterValueView propertyView = new GPropertyFilterValueView(this, propertyValue, logicsSupplier);
        valueViews.put(propertyValue, propertyView);

        filterValues.add(dataValue, objectValue, propertyValue);
        filterValues.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                condition.value = (GFilterValue) filterValues.getSelectedValue();
                filterChanged();
            }
        });
        add(filterValues);

        condition.value = (GFilterValue) filterValues.getSelectedValue();

        junctionView = new GFilterConditionListBox();
        junctionView.addStyleName("customFontPresenter");
        junctionView.add(new String[]{"И", "ИЛИ"});
        junctionView.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                condition.junction = junctionView.getSelectedIndex() == 0;
                handler.conditionChanged();
            }
        });
        add(junctionView);

        deleteButton = new ImageButton(null, DELETE);
        deleteButton.addStyleName("toolbarButton");
        deleteButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                handler.conditionRemoved(condition);
            }
        });
        add(deleteButton);

        filterChanged();
    }

    private void filterChanged() {
        if (valueView != null) {
            remove(valueView);
        }

        valueView = valueViews.get(condition.value);
        if (valueView != null) {
            insert(valueView, getWidgetIndex(junctionView));
            valueView.propertyChanged(condition.property);
        }
        compareView.setItems(condition.property.baseType.getFilterCompares());
        compareView.setSelectedItem(condition.property.baseType.getDefaultCompare());
        condition.compare = (GCompare) compareView.getSelectedValue();

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
}
