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
import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.ComponentWidget;
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

public class GFilterConditionView extends FlexPanel implements HasNativeSID {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    public interface UIHandler {
        void addEnterBinding(Widget widget);
        void removeCondition(GPropertyFilter condition);
        void applyFilters(boolean focusFirstComponent, GFilterConditionView changedView);
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

    private ColumnsProvider columnsProvider;

    public boolean allowNull;

    private boolean isLast = false;
    private final UIHandler uiHandler;
    private boolean toolsVisible;

    // may not be applied without "Allow NULL", but we want to keep condition visible
    public boolean isConfirmed;

    private static int idCounter = 0;
    private final String sID;

    @Override
    public String getNativeSID() {
        return sID;
    }

    public boolean isRemoved;

    public GFilterConditionView(GPropertyFilter iCondition, GTableController logicsSupplier, final UIHandler uiHandler, ColumnsProvider columnsProvider, boolean toolsVisible, boolean readSelectedValue) {
        this.condition = iCondition;
        this.uiHandler = uiHandler;
        this.columnsProvider = columnsProvider;
        this.toolsVisible = toolsVisible;

        this.sID = "" + (idCounter++);

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
                applyFilters();
            }

            @Override
            public void allowNullChanged(boolean value) {
                allowNull = value;
                applyFilters();
            }

            @Override
            public void valueChanged(GCompare value) {
                super.valueChanged(value);
                condition.compare = value;
                updateCompareLabelText();
                valueView.changeCompare(value);
                applyFilters();
            }
        };
        compareView.setSelectedValue(condition.compare);
        compareView.setVisible(!isFixed() || toolsVisible);
        leftPanel.addCentered(compareView);

        valueView = new GDataFilterValueView(condition.value, logicsSupplier) {
            @Override
            public void valueChanged(Object value) {
                super.valueChanged(value);
                applyFilters(cell.enterPressed);
            }

            @Override
            public void editingCancelled(CancelReason cancelReason) {
                super.editingCancelled(cancelReason);
                if (!isConfirmed && !isFixed() && cancelReason == CancelReason.ESCAPE_PRESSED) {
                    GFilterConditionView.this.remove();
                }
            }
        };
        valueView.changeProperty(condition, readSelectedValue);

        rightPanel.addCentered(valueView);
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
                    applyFilters();
                };
            }
        };
        junctionView.addStyleName("userFilterButton");
        junctionView.addStyleName("userFilterJunctionSeparatorButton");
        junctionView.getElement().getStyle().setPaddingTop(0, Style.Unit.PX);
        junctionView.showBackground(!condition.junction);
        rightPanel.addCentered(junctionView);
    }

    private void applyFilters() {
        applyFilters(false);
    }
    private void applyFilters(boolean focusFirstComponent) {
        uiHandler.applyFilters(focusFirstComponent, this);
    }

    public ComponentWidget initView() {
        boolean alignCaptions = condition.filter.container.isAlignCaptions();
        if(alignCaptions) {
            addCentered(rightPanel);

            return new ComponentWidget(new SizedWidget(this), new CaptionWidget(new SizedWidget(leftPanel), GFlexAlignment.START, GFlexAlignment.CENTER));
        }

        addCentered(leftPanel);
        addCentered(rightPanel);
        return new ComponentWidget(this);
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

    public void onAdd() {
        valueView.onAdd();
    }

    public void startEditing(Event keyEvent) {
        // scheduleDeferred to fix focus issues with quick filter (adding condition by char key) 
        Scheduler.get().scheduleDeferred(() -> valueView.startEditing(keyEvent));
    }

    public boolean clearValueView() {
        if(valueView.cell.getValue() == null)
            return false;

        valueView.cell.updateValue(null);
        setApplied(allowNull);

        return true;
    }

    public void updateLoading(boolean loading) {
        valueView.cell.updateLoading(loading);
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
