package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.design.view.flex.CaptionContainerHolder;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GFilterConditionView extends FlexPanel implements CaptionContainerHolder {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    public interface UIHandler {
        void addEnterBinding(Widget widget);
        void removeCondition(GPropertyFilter condition);
        void applyFilters(boolean focusFirstComponent);
    }

    private static final String DELETE_ICON_PATH = "filtdel.png";
    private static final String SEPARATOR_ICON_PATH = "filtseparator.png";

    private GPropertyFilter condition;

    private Map<Column, String> columns = new HashMap<>();
    
    private Label propertyLabel;
    private GFilterOptionSelector<Column> propertyView;

    private Label compareLabel;
    private GFilterCompareSelector compareView;

    private GDataFilterValueView valueView;

    private GToolbarButton deleteButton;

    private Widget junctionSeparator;
    private GToolbarButton junctionView;
    
    private FlexPanel leftPanel;
    private FlexPanel rightPanel; 
    private LinearCaptionContainer captionContainer;

    public boolean allowNull;

    private boolean isLast = false;
    private boolean toolsVisible;

    // may not be applied without "Allow NULL", but we want to keep condition visible
    public boolean isConfirmed;

    public GFilterConditionView(GPropertyFilter iCondition, GTableController logicsSupplier, final UIHandler handler, boolean toolsVisible, boolean readSelectedValue) {
        this.condition = iCondition;
        this.toolsVisible = toolsVisible;

        allowNull = !condition.isFixed();
        
        leftPanel = new FlexPanel();
        rightPanel = new FlexPanel();

        List<Pair<Column, String>> selectedColumns = logicsSupplier.getSelectedColumns();
        for (Pair<Column, String> column : selectedColumns) {
            columns.put(column.first, column.second);
        }

        Column currentColumn = new Column(condition.property, condition.columnKey);
        String currentCaption = columns.get(currentColumn);
        
        propertyLabel = new Label(currentCaption);
        propertyLabel.addStyleName("userFilterLabel");
        leftPanel.addCentered(propertyLabel);

        propertyView = new GFilterOptionSelector<Column>(new Column[0]) {
            @Override
            public void valueChanged(Column column) {
                condition.property = column.property;
                condition.columnKey = column.columnKey;

                propertyLabel.setText(columns.get(column));

                propertyChanged();

                startEditing(GKeyStroke.createAddUserFilterKeyEvent());
            }
        };
        for (Pair<Column, String> column : selectedColumns) {
            propertyView.add(column.first, column.second);
        }
        propertyView.setSelectedValue(currentColumn, currentCaption);
        leftPanel.addCentered(propertyView);
        
        compareLabel = new Label();
        updateCompareLabelText();
        compareLabel.addStyleName("userFilterLabel");
        leftPanel.addCentered(compareLabel);

        compareView = new GFilterCompareSelector(condition, allowNull) {
            @Override
            public void negationChanged(boolean value) {
                condition.negation = value;
                updateCompareLabelText();
                handler.applyFilters(false);
            }

            @Override
            public void allowNullChanged(boolean value) {
                allowNull = value;
                handler.applyFilters(false);
            }

            @Override
            public void valueChanged(GCompare value) {
                super.valueChanged(value);
                condition.compare = value;
                updateCompareLabelText();
                handler.applyFilters(false);
            }
        };
        compareView.setSelectedValue(condition.compare);
        leftPanel.addCentered(compareView);

        valueView = new GDataFilterValueView(condition.value, logicsSupplier) {
            @Override
            public void valueChanged(Object value) {
                super.valueChanged(value);
                handler.applyFilters(cell.enterPressed);
            }

            @Override
            public void editingCancelled() {
                super.editingCancelled();
                if (!isConfirmed && !isFixed()) {
                    handler.removeCondition(condition);
                }
            }
        };
        rightPanel.addCentered(valueView);
        valueView.changeProperty(condition.property, condition.columnKey, readSelectedValue); // it's important to do it after adding to the container because setStatic -> setBaseSize is called inside (and adding to container also calls it and override with default value)
        handler.addEnterBinding(valueView.cell);

        deleteButton = new GToolbarButton(DELETE_ICON_PATH, messages.formFilterRemoveCondition()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> handler.removeCondition(condition);
            }
        };
        deleteButton.addStyleName("userFilterButton");
        rightPanel.addCentered(deleteButton);

        junctionSeparator = GwtClientUtils.createVerticalSeparator(StyleDefaults.COMPONENT_HEIGHT);
        junctionSeparator.addStyleName("userFilterJunctionSeparator");
        rightPanel.addCentered(junctionSeparator);

        junctionView = new GToolbarButton(SEPARATOR_ICON_PATH, messages.formFilterConditionViewOr()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    condition.junction = !condition.junction;
                    showBackground(!condition.junction);
                    handler.applyFilters(false);
                };
            }
        };
        junctionView.addStyleName("userFilterButton");
        junctionView.getElement().getStyle().setPaddingTop(0, Style.Unit.PX);
        junctionView.showBackground(!condition.junction);
        rightPanel.addCentered(junctionView);
    }
    
    public void initView() {
        if (captionContainer == null) {
            addCentered(leftPanel);
        } else {
            captionContainer.put(leftPanel, null, valueView.setBaseSize(), GFlexAlignment.CENTER);
        }
        
        addCentered(rightPanel);
    }
    
    public boolean isFixed() {
        return condition.isFixed();
    }

    private void updateCompareLabelText() {
        String negationString = condition.negation ? "!" : "";
        compareLabel.setText(negationString + condition.compare);
        compareLabel.setTitle(negationString + condition.compare.getTooltipText());
    }

    @Override
    protected void onAttach() {
        super.onAttach();

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

        compareLabel.setVisible(!toolsVisible);
        compareView.setVisible(toolsVisible);

        updateJunctionVisibility();
    }
    
    private void updateJunctionVisibility() {
        junctionSeparator.setVisible(!toolsVisible && !isLast && !condition.junction);
        junctionView.setVisible(toolsVisible && !isLast);
    }
    
    private void propertyChanged() {
        valueView.changeProperty(condition.property, condition.columnKey);
        
        GCompare oldCompare = condition.compare;
        GCompare[] filterCompares = condition.property.getFilterCompares();
        compareView.set(filterCompares);
        if (Arrays.asList(filterCompares).contains(oldCompare)) {
            compareView.setSelectedValue(oldCompare);
        } else {
            GCompare defaultCompare = condition.property.getDefaultCompare();
            compareView.setSelectedValue(defaultCompare);
            condition.compare = defaultCompare;

            updateCompareLabelText();
        }
    }

    public void focusOnValue() {
        valueView.focusOnValue();
    }

    public void startEditing(Event keyEvent) {
        // scheduleDeferred to fix focus issues with quick filter (adding condition by char key) 
        Scheduler.get().scheduleDeferred(() -> valueView.startEditing(keyEvent));
    }

    public void clearValueView() {
        valueView.cell.updateValue(null);
        setApplied(allowNull);
    }

    public void setApplied(boolean applied) {
        valueView.setApplied(applied);
    }

    @Override
    public void setCaptionContainer(LinearCaptionContainer captionContainer) {
        this.captionContainer = captionContainer;
    }

    @Override
    public GFlexAlignment getCaptionHAlignment() {
        return GFlexAlignment.END;
    }
}
