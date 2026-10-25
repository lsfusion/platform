package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.controller.GFormController;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GFilterConditionView extends FlexPanel implements HasNativeSID {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    // what a row reports to what draws it (GPlatformFiltersController), which decides what it does - the model's
    // manual apply mode: its condition changed - its value committed, its comparison, NOT, NULL allowed, junction -,
    // whether its value takes the focus then; another property chosen; its delete button, or Escape in an editor no
    // value was committed in; Enter in its value cell; and Escape in its selectors, with no popup shown
    public interface UIHandler {
        void conditionChanged(GFilterConditionView view, boolean focusValue);
        void propertyChanged(GFilterConditionView view);
        void removeCondition(GFilterConditionView view);
        void enterPressed(GFilterConditionView view);
        void resetConditions();
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

    public void destroy() {
        valueView.destroy();
    }

    public GFilterConditionView(GPropertyFilter iCondition, GFormController form, GTableController rows, UIHandler uiHandler, ColumnsProvider columnsProvider, boolean controlsVisible) {
        this.condition = iCondition;
        this.uiHandler = uiHandler;
        this.columnsProvider = columnsProvider;
        this.controlsVisible = controlsVisible;

        this.sID = "" + (idCounter++);
        
       GwtClientUtils.addClassName(this, "filter");

        allowNull = !condition.isFixed();
        
        leftPanel = new FlexPanel();
        rightPanel = new FlexPanel();
        GwtClientUtils.addClassName(rightPanel, "btn-toolbar");

        Column currentColumn = getCaptionColumn();
        String currentCaption = columnsProvider.getColumns().get(currentColumn);
        
        propertyLabel = new Label(currentCaption);
        propertyLabel.setTitle(currentCaption);
        GwtClientUtils.addClassName(propertyLabel, "filter-label");
        leftPanel.addCentered(propertyLabel);

        propertyView = new GFilterOptionSelector<Column>(uiHandler::resetConditions) {
            @Override
            public void valueChanged(Column column) {
                condition.column = column;
                // the group of its property, as the server takes it
                condition.groupObject = column.property.groupObject;

                String columnCaption = columnsProvider.getColumns().get(column);
                propertyLabel.setText(columnCaption);
                propertyLabel.setTitle(columnCaption);

                propertyChanged();

                startEditing(GKeyStroke.createAddUserFilterKeyEvent());
            }
        };
        for (Pair<Column, String> column : rows.getFilterColumns()) {
            propertyView.add(column.first, column.second);
        }
        propertyView.setSelectedValue(currentColumn, currentCaption);
        leftPanel.addCentered(propertyView);
        
        compareLabel = new Label();
        updateCompareLabelText();
        GwtClientUtils.addClassName(compareLabel, "filter-label");
        compareLabel.setVisible(isFixed() && !controlsVisible);
        leftPanel.addCentered(compareLabel);

        GCompare[] filterCompares = condition.column.property.getFilterCompares();
        List<String> conditionsFullStrings = new ArrayList<>();
        for (GCompare filterCompare : filterCompares) {
            conditionsFullStrings.add(filterCompare.getFullString());
        }
        compareView = new GFilterCompareSelector(condition, uiHandler::resetConditions, Arrays.asList(filterCompares), conditionsFullStrings, allowNull) {
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

        valueView = new GDataFilterValueView(condition.value, form, rows) {
            @Override
            public void valueChanged(PValue value) {
                super.valueChanged(value);
                confirmed = true;

                conditionChanged(false);
            }

            // Enter in its value cell
            @Override
            public void enterPressed() {
                uiHandler.enterPressed(GFilterConditionView.this);
            }

            @Override
            public void editingCancelled(CancelReason cancelReason) {
                super.editingCancelled(cancelReason);
                if (!confirmed && !isFixed() && cancelReason == CancelReason.ESCAPE_PRESSED) {
                    GFilterConditionView.this.remove();
                }
            }
        };
        valueView.changeProperty(condition, false);

        rightPanel.addCentered(valueView);

        deleteButton = new GToolbarButton(StaticImage.DELETE_FILTER, messages.formFilterRemoveCondition()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> GFilterConditionView.this.remove();
            }
        };
        GwtClientUtils.addClassName(deleteButton, "filter-button");
        deleteButton.setVisible(!isFixed()); // a FILTER's row is never removed: the FILTER is the design's
        rightPanel.add(deleteButton, GFlexAlignment.CENTER);

        junctionSeparator = new SimplePanel();
        junctionSeparator.getElement().setInnerText("｜");
        rightPanel.addCentered(junctionSeparator);

        junctionView = new GToolbarButton(
                MainFrame.useTextAsFilterSeparator ? messages.formFilterConditionViewAnd() : null,
                MainFrame.useTextAsFilterSeparator ? null : StaticImage.FILTER_SEPARATOR,
                messages.formFilterConditionViewAnd()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    condition.junction = !condition.junction;
                    updateJunctionView();

                    conditionChanged(true);
                };
            }
        };
        GwtClientUtils.addClassName(junctionView, "filter-button");
        GwtClientUtils.addClassName(junctionView, MainFrame.useTextAsFilterSeparator ? "filter-separator-button-text" : "filter-separator-button");
        updateJunctionView();
        rightPanel.addCentered(junctionView);
    }

    private void conditionChanged(boolean focusValueView) {
        uiHandler.conditionChanged(this, focusValueView);
    }

    // its junction shown: AND or OR, as text or as the button's background
    private void updateJunctionView() {
        String caption = condition.junction ? messages.formFilterConditionViewAnd() : messages.formFilterConditionViewOr();
        if (MainFrame.useTextAsFilterSeparator)
            junctionView.setText(caption);
        else
            junctionView.showBackground(!condition.junction);
        junctionView.setTitle(caption);
    }

    public void focusValueView() {
        // focus value view in order to be able to apply filter by pressing Enter (even if focus was somewhere else before)
        valueView.focusOnValue();
    }
    

    // the row of a FILTER a React view places is one view, its caption inside: the host it goes to has no caption
    // column of a container's to align it in
    public ComponentWidget initView() {
        boolean alignCaptions = !condition.filter.isPlatformDrawn() && condition.filter.container.isAlignCaptions();
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
    // the condition it shows and edits, its own (show)
    public GPropertyFilter getCondition() {
        return condition;
    }

    private void updateCompareLabelText() {
        compareLabel.setText((condition.negation ? "!" : "") + condition.compare);
        compareLabel.setTitle((condition.negation ? messages.formFilterCompareNot() + " " : "") + condition.compare.getTooltipText());
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

        // a fixed row's property - a FILTER's - is the design's: its caption stays, no choice of another
        propertyLabel.setVisible(!controlsVisible || isFixed());
        propertyView.setVisible(controlsVisible && !isFixed());

        if (isFixed()) {
            compareLabel.setVisible(!controlsVisible);
            compareView.setVisible(controlsVisible);
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
        GCompare[] filterCompares = condition.column.property.getFilterCompares();
        compareView.set(filterCompares);
        if (Arrays.asList(filterCompares).contains(oldCompare)) {
            compareView.setSelectedValue(oldCompare);
        } else {
            GCompare defaultCompare = condition.column.property.getDefaultCompare();
            compareView.setSelectedValue(defaultCompare);
            condition.compare = defaultCompare;

            updateCompareLabelText();
        }
        
        uiHandler.propertyChanged(this);
    }

    // what it shows of its entry in the table's user filters (GUserFiltersController), as the model has it now - each
    // part set where it differs, so a row's own change told back to it leaves its editor alone: the column - its
    // caption, its choice, a value cell built anew for another -, the comparison - the selector, the label, the value's
    // input list -, NOT, the value, the junction, the NULL allowed, and whether a value was committed (Escape takes
    // away a row with none)
    public void show(GPropertyFilter entry, boolean allowNull, boolean confirmed) {
        if (!entry.column.equals(condition.column)) {
            condition.column = entry.column;
            condition.groupObject = entry.groupObject;
            String caption = columnsProvider.getColumns().get(getCaptionColumn());
            propertyLabel.setText(caption);
            propertyLabel.setTitle(caption);
            propertyView.setSelectedValue(getCaptionColumn(), caption);
            compareView.set(condition.column.property.getFilterCompares());
            valueView.changeProperty(condition, false);
        }
        if (entry.compare != condition.compare) {
            condition.compare = entry.compare;
            compareView.setSelectedValue(entry.compare);
            updateCompareLabelText();
            valueView.changeCompare(entry.compare);
        }
        if (entry.negation != condition.negation) {
            condition.negation = entry.negation;
            compareView.setNegation(entry.negation);
            updateCompareLabelText();
        }
        if (!Objects.equals(entry.value.value, condition.value.value))
            valueView.setValue(entry.value.value);
        if (entry.junction != condition.junction) {
            condition.junction = entry.junction;
            updateJunctionView();
            updateJunctionVisibility();
        }
        if (allowNull != this.allowNull)
            setAllowNull(allowNull);
        this.confirmed = confirmed;
    }

    public void setAllowNull(boolean allowNull) {
        this.allowNull = allowNull;
        compareView.setAllowNull(allowNull);
    }

    public void onAdd(boolean focusValueView) {
        valueView.onAdd(focusValueView);
    }

    public void startEditing(Event keyEvent) {
        // scheduleDeferred to fix focus issues with quick filter (adding condition by char key)
        // UPD (quick filter): with scheduleDeferred() keyUp event often doesn't reach suggest box and initial suggestions don't appear
        // As focus issue is not reproducible now, comment scheduleDeferred() out  
//        Scheduler.get().scheduleDeferred(() -> {
            valueView.startEditing(keyEvent);
//        });
    }

    // ... and its value's editor, if open, cancelled (GDataFilterValueView.cancelEditing)
    public void cancelEditing() {
        valueView.cancelEditing();
    }

    public void updateLoading(boolean loading) {
        valueView.cell.updateLoading(loading);
    }
    
    private void remove() {
        propertyView.hidePopup();
        compareView.hidePopup();
        uiHandler.removeCondition(this);
    }
    
    public void setApplied(boolean applied) {
        valueView.setApplied(applied);
    }

    // the column the captions are by: one with no column key is the column of the current objects of the column groups
    private Column getCaptionColumn() {
        return condition.column.columnKey != null ? condition.column : new Column(condition.column.property, GGroupObjectValue.EMPTY);
    }

    // shown or hidden - by its SHOWIF, with its table - with its caption, a widget apart where its container aligns
    // captions (initView)
    @Override
    public void setVisible(boolean nVisible) {
        super.setVisible(nVisible);
        leftPanel.setVisible(nVisible);

        if (nVisible) {
            String columnCaption = columnsProvider.getColumns().get(getCaptionColumn());
            propertyLabel.setText(columnCaption);
            propertyLabel.setTitle(columnCaption);
        }
    }

    public interface ColumnsProvider {
        Map<Column, String> getColumns();
    }
}
