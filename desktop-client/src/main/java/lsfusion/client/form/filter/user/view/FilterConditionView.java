package lsfusion.client.form.filter.user.view;

import lsfusion.base.Pair;
import lsfusion.client.controller.MainController;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.design.view.Filler;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.flex.CaptionContainerHolder;
import lsfusion.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.client.form.design.view.widget.LabelWidget;
import lsfusion.client.form.design.view.widget.SeparatorWidget;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.user.controller.FilterController;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.property.panel.view.DataPanelView;
import lsfusion.client.form.view.Column;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.Compare;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;
import java.util.*;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.base.view.SwingDefaults.*;
import static lsfusion.client.form.object.ClientGroupObjectValue.EMPTY;

public class FilterConditionView extends FlexPanel implements CaptionContainerHolder {
    public interface UIHandler {
        void removeCondition(ClientPropertyFilter condition);
        void applyFilters(boolean focusFirstComponent);
        void enableApplyButton();
    }

    private TableController logicsSupplier;
    private final UIHandler uiHandler;

    private static final String DELETE_ICON_PATH = "filtdel.png";
    private static final String SEPARATOR_ICON_PATH = "filtseparator.png";

    private final ClientPropertyFilter condition;

    private LabelWidget propertyLabel;
    private FilterOptionSelector<Column> propertyView;

    private LabelWidget compareLabel;
    private FilterCompareSelector compareView;
    
    private DataFilterValueView valueView;
    
    private FlexPanel deleteButtonWrapper;
    
    private FlexPanel junctionSeparator;
    private FlexPanel junctionViewWrapper;

    private FlexPanel leftPanel;
    private FlexPanel rightPanel;
    private LinearCaptionContainer captionContainer;
    
    private final ColumnsProvider columnsProvider;

    public boolean allowNull;

    private boolean isLast = false;
    private boolean controlsVisible;
    
    // may not be applied without "Allow NULL", but we want to keep condition visible
    public boolean confirmed;

    private boolean innerValueChange = false;
    
    public FilterConditionView(ClientPropertyFilter ifilter, TableController logicsSupplier, UIHandler iuiHandler, ColumnsProvider columnsProvider, boolean controlsVisible, EventObject keyEvent, boolean readSelectedValue) {
        super(false, FlexAlignment.START);
        condition = ifilter;
        this.logicsSupplier = logicsSupplier;
        uiHandler = iuiHandler;
        this.columnsProvider = columnsProvider;
        this.controlsVisible = controlsVisible;

        allowNull = !condition.isFixed();

        leftPanel = new FlexPanel();
        rightPanel = new FlexPanel();

        Column currentColumn = new Column(condition.property, condition.columnKey != null ? condition.columnKey : EMPTY);
        String currentCaption = columnsProvider.getColumns().get(currentColumn);

        Border labelBorder = BorderFactory.createEmptyBorder(0,
                getTableCellMargins().left + getComponentBorderWidth(),
                0,
                getTableCellMargins().right + getComponentBorderWidth());
        
        propertyLabel = new LabelWidget(currentCaption);
        propertyLabel.setBorder(labelBorder);
        leftPanel.addCentered(propertyLabel);

        propertyView = new FilterOptionSelector<Column>(logicsSupplier) {
            @Override
            public void valueChanged(Column value) {
                condition.property = value.property;
                condition.columnKey = value.columnKey;

                propertyLabel.setText(columnsProvider.getColumns().get(value));
                
                propertyChanged();

                SwingUtilities.invokeLater(() -> startEditing(FilterController.createAddUserFilterEvent(valueView)));
            }
        };

        for (Pair<Column, String> column : logicsSupplier.getSelectedColumns()) {
            propertyView.add(column.first, column.second);
        }
        leftPanel.addCentered(propertyView);

        compareLabel = new LabelWidget();
        updateCompareLabelText();
        compareLabel.setBorder(labelBorder);
        compareLabel.setVisible(isFixed() && !controlsVisible);
        leftPanel.addCentered(compareLabel);

        Compare[] filterCompares = condition.property.getFilterCompares();
        List<String> conditionsFullStrings = new ArrayList<>();
        for (Compare filterCompare : filterCompares) {
            conditionsFullStrings.add(filterCompare.getFullString());
        }
        compareView = new FilterCompareSelector(logicsSupplier, condition, Arrays.asList(filterCompares), conditionsFullStrings, allowNull) {
            @Override
            public void valueChanged(Compare value) {
                condition.compare = value;
                updateCompareLabelText();
                valueView.changeCompare(value);
                enableApplyButton();
                focusValueView();
            }

            @Override
            public void negationChanged(boolean value) {
                condition.negation = value;
                updateCompareLabelText();
                enableApplyButton();
                
                FilterConditionView.this.logicsSupplier.getFormController().revalidate();
            }

            @Override
            public void allowNullChanged(boolean value) {
                enableApplyButton();
                allowNull = value;
            }

            @Override
            public void menuCanceled() {
                // catch menu cancel event instead of negationChanged and allowNullChanged for proper value view focus behavior
                focusValueView();
            }
        };
        compareView.setSelectedValue(condition.compare);
        compareView.setVisible(!isFixed() || controlsVisible);
        leftPanel.addCentered(compareView);

        valueView = new DataFilterValueView(condition, logicsSupplier, keyEvent, readSelectedValue) {
            @Override
            public void valueChanged(Object newValue) {
                super.valueChanged(newValue);
                if (!innerValueChange) { // to avoid multiple apply calls
                    enableApplyButton();
                    if (valueTable.editorEnterPressed() || !FilterConditionView.this.controlsVisible) {
                        applyFilters(valueTable.editorEnterPressed());
                    }
                    confirmed = true;
                }
            }

            @Override
            public void editingCancelled() {
                super.editingCancelled();
                if (!confirmed && !isFixed()) {
                    FilterConditionView.this.remove();
                }
            }
        };
        valueView.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 2));
        rightPanel.addCentered(valueView);

        deleteButtonWrapper = new FlexPanel(false, FlexAlignment.START);
        ToolbarGridButton deleteButton = new ToolbarGridButton(DELETE_ICON_PATH, getString("form.queries.filter.remove.condition"));
        deleteButton.addActionListener(e -> RmiQueue.runAction(FilterConditionView.this::remove));
        deleteButtonWrapper.add((Widget)deleteButton);
        deleteButtonWrapper.add(Filler.createHorizontalStrut(2));
        deleteButtonWrapper.setVisible(!isFixed() || controlsVisible);
        rightPanel.addCentered(deleteButtonWrapper);

        junctionSeparator = new FlexPanel(false, FlexAlignment.START);
        int separatorMarginWidth = getComponentHeight() / 2;
        junctionSeparator.add(Filler.createHorizontalStrut(separatorMarginWidth));
        junctionSeparator.add(new SeparatorWidget(SwingConstants.VERTICAL), FlexAlignment.STRETCH, 0.0);
        junctionSeparator.add(Filler.createHorizontalStrut(separatorMarginWidth - 2)); // 2 for margin compensation
        rightPanel.add(junctionSeparator, FlexAlignment.STRETCH, 0.0);

        junctionViewWrapper = new FlexPanel(false, FlexAlignment.START);
        ToolbarGridButton junctionView = new ToolbarGridButton(
                MainController.useTextAsFilterSeparator ? getString("form.queries.and") : null,
                MainController.useTextAsFilterSeparator ? null : SEPARATOR_ICON_PATH,
                getString("form.queries.and"), null) {
            @Override
            public void addListener() {
                addActionListener(e -> {
                    condition.junction = !condition.junction;
                    String caption = condition.junction ? getString("form.queries.and") : getString("form.queries.or");
                    if(MainController.useTextAsFilterSeparator) {
                        setText(caption);
                        logicsSupplier.getFormController().revalidate();
                    } else {
                        showBackground(!condition.junction);
                    }
                    setToolTipText(caption);

                    enableApplyButton();
                    focusValueView();
                });
            }
        };
        junctionView.showBackground(!condition.junction);
        junctionViewWrapper.add((Widget) junctionView);
        junctionViewWrapper.add(Filler.createHorizontalStrut(2));
        rightPanel.addCentered(junctionViewWrapper);

        setControlsVisible(controlsVisible);

        propertyView.setSelectedValue(currentColumn, currentCaption);
    }

    private void focusValueView() {
        // focus value view in order to be able to apply filter by pressing Enter (even if focus was somewhere else before)
        valueView.requestFocusInWindow();
    }

    private void applyFilters(boolean focusFirstComponent) {
        uiHandler.applyFilters(focusFirstComponent);
    }

    private void enableApplyButton() {
        uiHandler.enableApplyButton();
    }

    public void initView() {
        if (captionContainer == null) {
            addCentered(leftPanel);
        } else {
            captionContainer.put(leftPanel, new Pair<>(null, null), DataPanelView.setBaseSize(valueView.valueTable, true, condition.property), FlexAlignment.CENTER);
        }

        addCentered(rightPanel);
    }

    public boolean isFixed() {
        return condition.isFixed();
    }

    private void updateCompareLabelText() {
        compareLabel.setText((condition.negation ? "!" : "") + condition.compare);
        compareLabel.setToolTipText((condition.negation ? getString("form.queries.filter.condition.not") + " " : "") + condition.compare.getTooltipText());
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(30, super.getPreferredSize().height);
    }

    public void setLast(boolean isLast) {
        this.isLast = isLast;

        updateJunctionVisibility();
    }
    
    public void setControlsVisible(boolean visible) {
        this.controlsVisible = visible;

        propertyLabel.setVisible(!controlsVisible);
        propertyView.setVisible(controlsVisible);

        if (isFixed()) {
            compareLabel.setVisible(!controlsVisible);
            compareView.setVisible(controlsVisible);
            
            deleteButtonWrapper.setVisible(visible);
        }

        updateJunctionVisibility();
    }

    public void updateJunctionVisibility() {
        junctionSeparator.setVisible(!controlsVisible && !isLast && !condition.junction);
        junctionViewWrapper.setVisible(controlsVisible && !isLast);
    }

    void propertyChanged() {
        valueView.changeProperty(condition);

        Compare oldCompare = condition.compare;
        List<Compare> filterCompares = Arrays.asList(condition.property.getFilterCompares());
        compareView.set(filterCompares);
        if (filterCompares.contains(oldCompare)) {
            compareView.setSelectedValue(oldCompare);
        } else {
            Compare defaultCompare = condition.property.getDefaultCompare();
            compareView.setSelectedValue(defaultCompare);
            condition.compare = defaultCompare;

            updateCompareLabelText();
        }
        
        enableApplyButton();

        logicsSupplier.getFormController().revalidate();
    }

    public void startEditing(EventObject initFilterEvent) {
        valueView.startEditing(initFilterEvent);
    }

    public void clearValueView() {
        innerValueChange = true;
        valueView.valueTable.setValueAt(null, 0, 0);
        innerValueChange = false;
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
    public FlexAlignment getCaptionHAlignment() {
        return FlexAlignment.END;
    }

    @Override
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        
        if (isVisible) {
            propertyLabel.setText(columnsProvider.getColumns().get(new Column(condition.property, condition.columnKey != null ? condition.columnKey : EMPTY)));
        }
    }
    
    public interface ColumnsProvider {
        Map<Column, String> getColumns();
    } 
}
