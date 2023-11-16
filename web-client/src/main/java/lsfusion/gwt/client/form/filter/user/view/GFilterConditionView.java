package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.design.view.ComponentWidget;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static lsfusion.gwt.client.view.StyleDefaults.COMPONENT_HEIGHT;

public class GFilterConditionView extends FlexPanel implements HasNativeSID {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    public interface UIHandler {
        void addEnterBinding(Widget widget);
        void removeCondition(GPropertyFilter condition);
        void applyFilters(boolean focusFirstComponent, GFilterConditionView changedView);
        void enableApplyButton();
        void resetConditions();
        boolean isManualApplyMode();
    }

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
    private boolean controlsVisible;

    // may not be applied without "Allow NULL", but we want to keep condition visible
    public boolean confirmed;

    private static int idCounter = 0;
    private final String sID;

    @Override
    public String getNativeSID() {
        return sID;
    }

    public boolean isRemoved;

    public GFilterConditionView(GPropertyFilter iCondition, GTableController logicsSupplier, UIHandler uiHandler, ColumnsProvider columnsProvider, boolean controlsVisible, boolean readSelectedValue) {
        this.condition = iCondition;
        this.uiHandler = uiHandler;
        this.columnsProvider = columnsProvider;
        this.controlsVisible = controlsVisible;

        this.sID = "" + (idCounter++);
        
        addStyleName("filter");

        allowNull = !condition.isFixed();
        
        leftPanel = new FlexPanel();
        rightPanel = new FlexPanel();
        rightPanel.addStyleName("btn-toolbar");

        Column currentColumn = new Column(condition.property, condition.columnKey != null ? condition.columnKey : GGroupObjectValue.EMPTY);
        String currentCaption = columnsProvider.getColumns().get(currentColumn);
        
        propertyLabel = new Label(currentCaption);
        propertyLabel.setTitle(currentCaption);
        propertyLabel.addStyleName("filter-label");
        leftPanel.addCentered(propertyLabel);

        propertyView = new GFilterOptionSelector<Column>(uiHandler) {
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
        for (Pair<Column, String> column : logicsSupplier.getFilterColumns()) {
            propertyView.add(column.first, column.second);
        }
        propertyView.setSelectedValue(currentColumn, currentCaption);
        leftPanel.addCentered(propertyView);
        
        compareLabel = new Label();
        updateCompareLabelText();
        compareLabel.addStyleName("filter-label");
        compareLabel.setVisible(isFixed() && !controlsVisible);
        leftPanel.addCentered(compareLabel);

        GCompare[] filterCompares = condition.property.getFilterCompares();
        List<String> conditionsFullStrings = new ArrayList<>();
        for (GCompare filterCompare : filterCompares) {
            conditionsFullStrings.add(filterCompare.getFullString());
        }
        compareView = new GFilterCompareSelector(condition, uiHandler, Arrays.asList(filterCompares), conditionsFullStrings, allowNull) {
            @Override
            public void negationChanged(boolean value) {
                condition.negation = value;
                updateCompareLabelText();

                conditionChanged(true);
            }

            @Override
            public void allowNullChanged(boolean value) {
                allowNull = value;

                conditionChanged(true);
            }

            @Override
            public void valueChanged(GCompare value) {
                super.valueChanged(value);
                condition.compare = value;
                updateCompareLabelText();
                valueView.changeCompare(value);

                conditionChanged(true);
            }
        };
        compareView.setSelectedValue(condition.compare);
        compareView.setVisible(!isFixed() || controlsVisible);
        leftPanel.addCentered(compareView);

        valueView = new GDataFilterValueView(condition.value, logicsSupplier) {
            @Override
            public void valueChanged(PValue value) {
                super.valueChanged(value);

                conditionChanged(false);
                
                confirmed = true;
            }

            @Override
            public void editingCancelled(CancelReason cancelReason) {
                super.editingCancelled(cancelReason);
                if (!confirmed && !isFixed() && cancelReason == CancelReason.ESCAPE_PRESSED) {
                    GFilterConditionView.this.remove();
                }
            }
        };
        valueView.changeProperty(condition, readSelectedValue);

        rightPanel.addCentered(valueView);
        uiHandler.addEnterBinding(valueView.cell);

        deleteButton = new GToolbarButton(StaticImage.DELETE_FILTER, messages.formFilterRemoveCondition()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> GFilterConditionView.this.remove();
            }
        };
        deleteButton.addStyleName("filter-button");
        deleteButton.setVisible(!isFixed() || controlsVisible);
        rightPanel.add(deleteButton, GFlexAlignment.CENTER, 0, false, GSize.CONST(COMPONENT_HEIGHT));

        junctionSeparator = GwtClientUtils.createVerticalSeparator(StyleDefaults.COMPONENT_HEIGHT);
        junctionSeparator.addStyleName("filter-separator");
        rightPanel.addCentered(junctionSeparator);

        junctionView = new GToolbarButton(
                MainFrame.useTextAsFilterSeparator ? messages.formFilterConditionViewAnd() : null,
                MainFrame.useTextAsFilterSeparator ? null : StaticImage.FILTER_SEPARATOR,
                messages.formFilterConditionViewAnd(), true) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    condition.junction = !condition.junction;
                    String caption = condition.junction ? messages.formFilterConditionViewAnd() : messages.formFilterConditionViewOr();
                    if(MainFrame.useTextAsFilterSeparator) {
                        setText(caption);
                    } else {
                        showBackground(!condition.junction);
                    }
                    setTitle(caption);

                    conditionChanged(true);
                };
            }
        };
        junctionView.addStyleName("filter-button");
        junctionView.addStyleName(MainFrame.useTextAsFilterSeparator ? "filter-separator-button-text" : "filter-separator-button");
        junctionView.showBackground(!condition.junction);
        rightPanel.addCentered(junctionView);
    }

    private void conditionChanged(boolean focusValueView) {
        if (uiHandler.isManualApplyMode()) {
            enableApplyButton();
            if (focusValueView) {
                focusValueView();
            }
        } else {
            uiHandler.applyFilters(true, this);
        }
    }

    public void focusValueView() {
        // focus value view in order to be able to apply filter by pressing Enter (even if focus was somewhere else before)
        valueView.focusOnValue();
    }
    

    public ComponentWidget initView() {
        boolean alignCaptions = condition.filter.container.isAlignCaptions();
        if(alignCaptions) {
            addCentered(rightPanel);

            return new ComponentWidget(this, leftPanel);
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
    
    private void enableApplyButton() {
        uiHandler.enableApplyButton();
    }

    @Override
    protected void onAttach() {
        super.onAttach();

        setControlsVisible(controlsVisible);
    }

    public void setLast(boolean isLast) {
        this.isLast = isLast;

        updateJunctionVisibility();
    }

    public void setControlsVisible(boolean visible) {
        controlsVisible = visible;

        propertyLabel.setVisible(!controlsVisible);
        propertyView.setVisible(controlsVisible);

        if (isFixed()) {
            compareLabel.setVisible(!controlsVisible);
            compareView.setVisible(controlsVisible);

            deleteButton.setVisible(visible);
        }

        updateJunctionVisibility();
    }
    
    private void updateJunctionVisibility() {
        junctionSeparator.setVisible(!controlsVisible && !isLast && !condition.junction);
        junctionView.setVisible(controlsVisible && !isLast);
    }
    
    private void propertyChanged() {
        valueView.changeProperty(condition, true);

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
        
        enableApplyButton();
    }

    public void onAdd(boolean focusValueView) {
        valueView.onAdd(focusValueView);
    }

    public void putSelectedValue() {
        if (!condition.property.differentValue) {
            valueView.putSelectedValue(condition);
        }
    }
    
    public void startEditing(Event keyEvent) {
        // scheduleDeferred to fix focus issues with quick filter (adding condition by char key)
        // UPD (quick filter): with scheduleDeferred() keyUp event often doesn't reach suggest box and initial suggestions don't appear
        // As focus issue is not reproducible now, comment scheduleDeferred() out  
//        Scheduler.get().scheduleDeferred(() -> {
            valueView.startEditing(keyEvent);
//        });
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
