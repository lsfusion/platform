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
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.ArrayList;
import java.util.Arrays;
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
    
    private ColumnsProvider columnsProvider;

    public boolean allowNull;

    private boolean isLast = false;
    private final UIHandler uiHandler;
    private boolean toolsVisible;

    // may not be applied without "Allow NULL", but we want to keep condition visible
    public boolean isConfirmed;

    public GFilterConditionView(GPropertyFilter iCondition, GTableController logicsSupplier, final UIHandler uiHandler, ColumnsProvider columnsProvider, boolean toolsVisible, boolean readSelectedValue) {
        this.condition = iCondition;
        this.uiHandler = uiHandler;
        this.columnsProvider = columnsProvider;
        this.toolsVisible = toolsVisible;

        allowNull = !condition.isFixed();
        
        leftPanel = new FlexPanel();
        rightPanel = new FlexPanel();

        Column currentColumn = new Column(condition.property, condition.columnKey != null ? condition.columnKey : GGroupObjectValue.EMPTY);
        String currentCaption = columnsProvider.getColumns().get(currentColumn);
        
        propertyLabel = new Label(currentCaption);
        propertyLabel.setTitle(currentCaption);
        propertyLabel.addStyleName("userFilterLabel");
        leftPanel.addCentered(propertyLabel);

        propertyView = new GFilterOptionSelector<Column>() {
            @Override
            public void valueChanged(Column column) {
                condition.property = column.property;
                condition.columnKey = column.columnKey;

                String columnCaption = columnsProvider.getColumns().get(column);
                propertyLabel.setText(columnCaption);
                propertyLabel.setTitle(columnCaption);

                propertyChanged();

                startEditing(GKeyStroke.createAddUserFilterKeyEvent());
            }
        };
        for (Pair<Column, String> column : logicsSupplier.getSelectedColumns()) {
            propertyView.add(column.first, column.second);
        }
        propertyView.setSelectedValue(currentColumn, currentCaption);
        leftPanel.addCentered(propertyView);
        
        compareLabel = new Label();
        updateCompareLabelText();
        compareLabel.addStyleName("userFilterLabel");
        compareLabel.setVisible(isFixed() && !toolsVisible);
        leftPanel.addCentered(compareLabel);

        GCompare[] filterCompares = condition.property.getFilterCompares();
        List<String> conditionsFullStrings = new ArrayList<>();
        for (GCompare filterCompare : filterCompares) {
            conditionsFullStrings.add(filterCompare.getFullString());
        }
        compareView = new GFilterCompareSelector(condition, Arrays.asList(filterCompares), conditionsFullStrings, allowNull) {
            @Override
            public void negationChanged(boolean value) {
                condition.negation = value;
                updateCompareLabelText();
                uiHandler.applyFilters(false);
            }

            @Override
            public void allowNullChanged(boolean value) {
                allowNull = value;
                uiHandler.applyFilters(false);
            }

            @Override
            public void valueChanged(GCompare value) {
                super.valueChanged(value);
                condition.compare = value;
                updateCompareLabelText();
                valueView.changeCompare(value);
                uiHandler.applyFilters(false);
            }
        };
        compareView.setSelectedValue(condition.compare);
        compareView.setVisible(!isFixed() || toolsVisible);
        leftPanel.addCentered(compareView);

        valueView = new GDataFilterValueView(condition.value, logicsSupplier) {
            @Override
            public void valueChanged(Object value) {
                super.valueChanged(value);
                uiHandler.applyFilters(cell.enterPressed);
            }

            @Override
            public void editingCancelled(CancelReason cancelReason) {
                super.editingCancelled(cancelReason);
                if (!isConfirmed && !isFixed() && cancelReason == CancelReason.ESCAPE_PRESSED) {
                    GFilterConditionView.this.remove();
                }
            }
        };
        rightPanel.addCentered(valueView);
        valueView.changeProperty(condition, readSelectedValue); // it's important to do it after adding to the container because setStatic -> setBaseSize is called inside (and adding to container also calls it and override with default value)
        uiHandler.addEnterBinding(valueView.cell);

        deleteButton = new GToolbarButton(DELETE_ICON_PATH, messages.formFilterRemoveCondition()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> GFilterConditionView.this.remove();
            }
        };
        deleteButton.addStyleName("userFilterButton");
        deleteButton.setVisible(!isFixed() || toolsVisible);
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
                    uiHandler.applyFilters(false);
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
            captionContainer.put(leftPanel, GFlexAlignment.CENTER);
        }
        
        addCentered(rightPanel);
    }
    
    public boolean isFixed() {
        return condition.isFixed();
    }

    private void updateCompareLabelText() {
        compareLabel.setText((condition.negation ? "!" : "") + condition.compare);
        compareLabel.setTitle((condition.negation ? messages.formFilterCompareNot() + " " : "") + condition.compare.getTooltipText());
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

        propertyLabel.setVisible(!toolsVisible);
        propertyView.setVisible(toolsVisible);

        if (isFixed()) {
            compareLabel.setVisible(!toolsVisible);
            compareView.setVisible(toolsVisible);

            deleteButton.setVisible(visible);
        }

        updateJunctionVisibility();
    }
    
    private void updateJunctionVisibility() {
        junctionSeparator.setVisible(!toolsVisible && !isLast && !condition.junction);
        junctionView.setVisible(toolsVisible && !isLast);
    }
    
    private void propertyChanged() {
        valueView.changeProperty(condition);
        
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
    
    private void remove() {
        propertyView.hidePopup();
        compareView.hidePopup();
        uiHandler.removeCondition(condition);
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
        return GFlexAlignment.START;
    }

    @Override
    public void setVisible(boolean nVisible) {
        super.setVisible(nVisible);

        if (nVisible) {
            String columnCaption = columnsProvider.getColumns().get(new Column(condition.property, condition.columnKey != null ? condition.columnKey : GGroupObjectValue.EMPTY));
            propertyLabel.setText(columnCaption);
            propertyLabel.setTitle(columnCaption);
        }
    }

    public interface ColumnsProvider {
        Map<Column, String> getColumns();
    }
}
