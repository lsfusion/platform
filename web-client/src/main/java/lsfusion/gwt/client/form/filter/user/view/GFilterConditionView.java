package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.view.Column;

import java.util.Arrays;

public class GFilterConditionView extends FlexPanel {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    public interface UIHandler {
        void addEnterBinding(Widget widget);
        void conditionRemoved(GPropertyFilter condition);
        void applyFilter();
    }

    private static final String DELETE_ICON_PATH = "filtdel.png";

    private Label propertyLabel;
    private Label negationLabel;
    private Label compareLabel;
    private GDataFilterValueView valueView;
    private Label junctionLabel;
    
    private GToolbarButton deleteButton;

    private GFilterConditionListBox propertyView;
    private CheckBox negationView;
    private GFilterConditionListBox compareView;
    private GFilterConditionListBox junctionView;

    private boolean isLast = false;
    private boolean toolsVisible;

    public GPropertyFilter condition;
    
    private boolean focused = false;

    public GFilterConditionView(GPropertyFilter iCondition, GTableController logicsSupplier, final UIHandler handler, boolean toolsVisible) {
        this.condition = iCondition;
        this.toolsVisible = toolsVisible;

        propertyLabel = new Label(condition.property.getNotEmptyCaption());
        propertyLabel.addStyleName("userFilterLabel");
        addCentered(propertyLabel);

        propertyView = new GFilterConditionListBox();
        propertyView.addStyleName("customFontPresenter");
        for (Pair<Column, String> column : logicsSupplier.getSelectedColumns()) {
            propertyView.add(column.first, column.second);
        }
        propertyView.addChangeHandler(event -> {
            Column selectedItem = (Column) propertyView.getSelectedItem();
            condition.property = selectedItem.property;
            condition.columnKey = selectedItem.columnKey;

            propertyLabel.setText(selectedItem.property.getNotEmptyCaption());

            propertyChanged();
        });

        if (condition.property != null) {
            propertyView.setSelectedItem(new Column(condition.property, condition.columnKey));
        }
        addCentered(propertyView);
        
        negationLabel = new Label(messages.formFilterConditionViewNot());
        negationLabel.addStyleName("userFilterLabel");
        addCentered(negationLabel);

        negationView = new CheckBox(messages.formFilterConditionViewNot());
        negationView.addStyleName("userFilterCheckBox");
        negationView.addValueChangeHandler(event -> {
            condition.negation = negationView.getValue();
        });
        negationView.setValue(condition.negation);
        addCentered(negationView);
        
        compareLabel = new Label(condition.compare.toString());
        compareLabel.addStyleName("userFilterLabel");
        addCentered(compareLabel);

        compareView = new GFilterConditionListBox();
        compareView.addStyleName("customFontPresenter");
        compareView.add((Object[]) GCompare.values());
        compareView.addChangeHandler(event -> {
            condition.compare = (GCompare) compareView.getSelectedItem();

            compareLabel.setText(condition.compare.toString());
        });
        compareView.setItems(condition.property.baseType.getFilterCompares());
        compareView.setSelectedItem(condition.compare);
        addCentered(compareView);

        GDataFilterValue dataValue = condition.value instanceof GDataFilterValue ? (GDataFilterValue) condition.value : new GDataFilterValue();
        valueView = new GDataFilterValueView(dataValue, condition.property, condition.columnKey, logicsSupplier) {
            @Override
            public void setFocused(boolean focused) {
                GFilterConditionView.this.focused = focused;
            }
        };
        handler.addEnterBinding(valueView.cell);
        addCentered(valueView);

        junctionLabel = new Label(condition.junction ? messages.formFilterConditionViewAnd() : messages.formFilterConditionViewOr());
        junctionLabel.addStyleName("userFilterLabel");
        addCentered(junctionLabel);

        junctionView = new GFilterConditionListBox();
        junctionView.addStyleName("customFontPresenter");
        junctionView.add(new Object[]{messages.formFilterConditionViewAnd(), messages.formFilterConditionViewOr()});
        junctionView.addChangeHandler(event -> {
            condition.junction = junctionView.getSelectedIndex() == 0;

            junctionLabel.setText(junctionView.getSelectedItemText());
        });
        junctionView.setSelectedIndex(condition.junction ? 0 : 1);
        addCentered(junctionView);
        
        deleteButton = new GToolbarButton(DELETE_ICON_PATH, messages.formQueriesFilterRemoveCondition()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    handler.conditionRemoved(condition);
                };
            }
        };
        deleteButton.addStyleName("userFilterButton");
        addCentered(deleteButton);
        
        setToolsVisible(toolsVisible);
    }

    public void setLast(boolean isLast) {
        this.isLast = isLast;

        updateJunctionVisibility();
    }

    public void setToolsVisible(boolean visible) {
        toolsVisible = visible;
        deleteButton.setVisible(visible);

        propertyLabel.setVisible(!toolsVisible);
        propertyView.setVisible(toolsVisible);

        negationLabel.setVisible(!toolsVisible && condition.negation);
        negationView.setVisible(toolsVisible);

        compareLabel.setVisible(!toolsVisible);
        compareView.setVisible(toolsVisible);

        updateJunctionVisibility();
    }
    
    private void updateJunctionVisibility() {
        junctionLabel.setVisible(!toolsVisible && !isLast);
        junctionView.setVisible(toolsVisible && !isLast);
    }
    
    public boolean isFocused() {
        return focused;
    }

    private void propertyChanged() {
        valueView.propertyChanged(condition);
        
        GCompare oldCompare = (GCompare) compareView.getSelectedItem();
        GCompare[] filterCompares = condition.property.baseType.getFilterCompares();
        compareView.setItems(filterCompares);
        if (Arrays.asList(filterCompares).contains(oldCompare)) {
            compareView.setSelectedItem(oldCompare);
        } else {
            GCompare defaultCompare = condition.property.getDefaultCompare();
            compareView.setSelectedItem(defaultCompare);
            condition.compare = defaultCompare;

            compareLabel.setText(defaultCompare.toString());
        }
    }

    public void focusOnValue() {
        valueView.focusOnValue();
    }

    public void startEditing(Event keyEvent) {
        valueView.startEditing(keyEvent);
    }
}
