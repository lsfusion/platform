package lsfusion.client.form.filter.user.view;

import lsfusion.base.Pair;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.view.Column;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.base.view.FlexLayout;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.property.Compare;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;
import java.util.*;

import static javax.swing.Box.createHorizontalStrut;
import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.base.view.SwingDefaults.*;

public class FilterConditionView extends JPanel {
    public interface UIHandler {
        void removeCondition(ClientPropertyFilter condition);
        void applyFilters(boolean focusFirstComponent);
    }

    private final UIHandler uiHandler;

    private static final String DELETE_ICON_PATH = "filtdel.png";
    private static final String SEPARATOR_ICON_PATH = "filtseparator.png";

    private final ClientPropertyFilter condition;

    private Map<Column, String> columns = new HashMap<>();
    
    private JLabel propertyLabel;
    private FilterOptionSelector<Column> propertyView;

    private JLabel compareLabel;
    private FilterCompareSelector compareView;
    
    private DataFilterValueView valueView;
    
    private JPanel deleteButtonWrapper;
    
    private JPanel junctionSeparator;
    private JPanel junctionViewWrapper;

    public boolean allowNull = false;

    private boolean isLast = false;
    private boolean toolsVisible;
    
    // may not be applied without "Allow NULL", but we want to keep condition visible
    public boolean isConfirmed;
    
    public FilterConditionView(ClientPropertyFilter ifilter, TableController logicsSupplier, UIHandler iuiHandler, boolean toolsVisible, boolean readSelectedValue) {
        condition = ifilter;
        uiHandler = iuiHandler;
        this.toolsVisible = toolsVisible;

        setLayout(new FlexLayout(this, false, FlexAlignment.START));

        List<Pair<Column, String>> selectedColumns = logicsSupplier.getSelectedColumns();
        for (Pair<Column, String> column : selectedColumns) {
            columns.put(column.first, column.second);
        }

        Column currentColumn = new Column(condition.property, condition.columnKey);
        String currentCaption = columns.get(currentColumn);

        Border labelBorder = BorderFactory.createEmptyBorder(0,
                getTableCellMargins().left + getComponentBorderWidth(),
                0,
                getTableCellMargins().right + getComponentBorderWidth());
        
        propertyLabel = new JLabel(currentCaption);
        propertyLabel.setBorder(labelBorder);
        addCentered(propertyLabel);

        propertyView = new FilterOptionSelector<Column>() {
            @Override
            public void valueChanged(Column value) {
                condition.property = value.property;
                condition.columnKey = value.columnKey;

                propertyLabel.setText(columns.get(value));
                
                propertyChanged();

                startEditing(KeyStrokes.createAddUserFilterKeyEvent(valueView));
            }
        };

        for (Pair<Column, String> column : selectedColumns) {
            propertyView.add(column.first, column.second);
        }
        addCentered(propertyView);

        compareLabel = new JLabel();
        updateCompareLabelText();
        compareLabel.setBorder(labelBorder);
        addCentered(compareLabel);

        compareView = new FilterCompareSelector(condition) {
            @Override
            public void valueChanged(Compare value) {
                condition.compare = value;
                updateCompareLabelText();
                uiHandler.applyFilters(false);
            }

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
        };
        compareView.setSelectedValue(condition.compare);
        addCentered(compareView);

        valueView = new DataFilterValueView(condition.value, condition.property, condition.columnKey, logicsSupplier, readSelectedValue) {
            @Override
            public void valueChanged(Object newValue) {
                super.valueChanged(newValue);
                uiHandler.applyFilters(valueTable.editorEnterPressed());
            }

            @Override
            public void applyFilters(boolean focusFirstComponent) {
                uiHandler.applyFilters(focusFirstComponent);
            }

            @Override
            public void editingCancelled() {
                super.editingCancelled();
                if (!isConfirmed) {
                    uiHandler.removeCondition(condition);
                }
            }
        };
        valueView.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        addCentered(valueView);

        deleteButtonWrapper = new JPanel();
        deleteButtonWrapper.setLayout(new FlexLayout(deleteButtonWrapper, false, FlexAlignment.START));
        ToolbarGridButton deleteButton = new ToolbarGridButton(DELETE_ICON_PATH, getString("form.queries.filter.remove.condition"), new Dimension(getComponentHeight(), getComponentHeight()));
        deleteButton.addActionListener(e -> RmiQueue.runAction(() -> uiHandler.removeCondition(condition)));
        deleteButtonWrapper.add(deleteButton, new FlexConstraints());
        deleteButtonWrapper.add(Box.createHorizontalStrut(2), new FlexConstraints());
        addCentered(deleteButtonWrapper);

        junctionSeparator = new JPanel();
        junctionSeparator.setLayout(new FlexLayout(junctionSeparator, false, FlexAlignment.START));
        int separatorMarginWidth = getComponentHeight() / 2;
        junctionSeparator.add(createHorizontalStrut(separatorMarginWidth), new FlexConstraints());
        junctionSeparator.add(new JSeparator(SwingConstants.VERTICAL), new FlexConstraints(FlexAlignment.STRETCH, 0));
        junctionSeparator.add(createHorizontalStrut(separatorMarginWidth - 2), new FlexConstraints()); // 2 for margin compensation 
        add(junctionSeparator, new FlexConstraints(FlexAlignment.STRETCH, 0));

        junctionViewWrapper = new JPanel();
        junctionViewWrapper.setLayout(new FlexLayout(junctionViewWrapper, false, FlexAlignment.START));
        ToolbarGridButton junctionView = new ToolbarGridButton(SEPARATOR_ICON_PATH, getString("form.queries.or")) {
            @Override
            public void addListener() {
                addActionListener(e -> {
                    condition.junction = !condition.junction;
                    showBackground(!condition.junction);
                    uiHandler.applyFilters(false);
                });
            }
        };
        junctionView.showBackground(!condition.junction);
        junctionViewWrapper.add(junctionView, new FlexConstraints());
        junctionViewWrapper.add(Box.createHorizontalStrut(2), new FlexConstraints());
        addCentered(junctionViewWrapper);

        setToolsVisible(toolsVisible);

        propertyView.setSelectedValue(currentColumn, currentCaption);
    }

    private void updateCompareLabelText() {
        String negationString = condition.negation ? "!" : "";
        compareLabel.setText(negationString + condition.compare);
        compareLabel.setToolTipText(negationString + condition.compare.getTooltipText());
    }

    private void addCentered(Component component) {
        add(component, new FlexConstraints(FlexAlignment.CENTER, 0));
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
    
    public void setToolsVisible(boolean visible) {
        this.toolsVisible = visible;
        deleteButtonWrapper.setVisible(visible);

        propertyLabel.setVisible(!toolsVisible);
        propertyView.setVisible(toolsVisible);

        compareLabel.setVisible(!toolsVisible);
        compareView.setVisible(toolsVisible);

        updateJunctionVisibility();
    }

    public void updateJunctionVisibility() {
        junctionSeparator.setVisible(!toolsVisible && !isLast && !condition.junction);
        junctionViewWrapper.setVisible(toolsVisible && !isLast);
    }

    void propertyChanged() {
        valueView.changeProperty(condition.property, condition.columnKey);

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

        validate();
    }

    public void startEditing(EventObject initFilterEvent) {
        valueView.startEditing(initFilterEvent);
    }
}
