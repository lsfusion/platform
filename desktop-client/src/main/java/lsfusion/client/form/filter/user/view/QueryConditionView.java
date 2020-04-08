package lsfusion.client.form.filter.user.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.view.ItemAdapter;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.filter.user.*;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.Compare;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static lsfusion.client.base.view.SwingDefaults.getComponentHeight;

public class QueryConditionView extends JPanel implements FilterValueListener {
    public interface UIHandlers {
        void conditionChanged();
        void conditionRemoved(ClientPropertyFilter condition);
        void applyQuery();
    }

    private final UIHandlers uiHandlers;

    // Icons - загружаем один раз, для экономии
    private static final String DELETE_ICON_PATH = "filtdel.png";

    private final ClientPropertyFilter filter;

    private final JComboBox cbFilterValues;

    private JPanel centerPanel;

    private FilterValueView valueView;
    private final Map<ClientFilterValue, FilterValueView> valueViews;

    private final JButton delButton;
    private JComboBox propertyView;
    private JComboBox compareView;
    private JCheckBox negationView;
    private JComboBox junctionView;

    public QueryConditionView(ClientPropertyFilter ifilter, TableController logicsSupplier, UIHandlers iuiHandlers) {

        setAlignmentX(LEFT_ALIGNMENT);

        filter = ifilter;
        uiHandlers = iuiHandlers;

        centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));

        setLayout(new BorderLayout());

        Vector<ClientPropertyDraw> sources = new Vector<>();
        sources.addAll(logicsSupplier.getGroupObjectProperties());

        propertyView = new QueryConditionComboBox(sources);
        centerPanel.add(propertyView);

        negationView = new JCheckBox(ClientResourceBundle.getString("form.queries.not"));
        negationView.setPreferredSize(new Dimension(negationView.getPreferredSize().width, getComponentHeight()));
        centerPanel.add(negationView);

        compareView = new QueryConditionComboBox(Compare.values());
        centerPanel.add(compareView);

        if (filter.property != null) {
            setSelectedPropertyDraw(filter.property);
        }
        filter.compare = (Compare) compareView.getSelectedItem();

        propertyView.addItemListener(new ItemAdapter() {
            public void itemSelected(ItemEvent e) {
                filter.property = (ClientPropertyDraw) e.getItem();
                filter.columnKey = null;
                filterChanged();
            }
        });

        negationView.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                filter.negation = negationView.isSelected();
                uiHandlers.conditionChanged();
            }
        });

        compareView.addItemListener(new ItemAdapter() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                filter.compare = (Compare) e.getItem();
                uiHandlers.conditionChanged();
            }
        });

        valueViews = new HashMap<>();

        ClientDataFilterValue dataValue = new ClientDataFilterValue();
        DataFilterValueView dataView = new DataFilterValueView(this, dataValue, filter.property, logicsSupplier) {
            @Override
            public void applyQuery() {
                uiHandlers.applyQuery();
            }
        };
        valueViews.put(dataValue, dataView);

        ClientObjectFilterValue objectValue = new ClientObjectFilterValue();
        ObjectFilterValueView objectView = new ObjectFilterValueView(this, objectValue, logicsSupplier);
        valueViews.put(objectValue, objectView);

        ClientPropertyFilterValue propertyValue = new ClientPropertyFilterValue();
        PropertyFilterValueView propertyValueView = new PropertyFilterValueView(this, propertyValue, logicsSupplier);
        valueViews.put(propertyValue, propertyValueView);

        ClientFilterValue[] filterValues = new ClientFilterValue[]{dataValue, objectValue, propertyValue};
        cbFilterValues = new QueryConditionComboBox(filterValues);
        centerPanel.add(cbFilterValues);

        filter.value = (ClientFilterValue) cbFilterValues.getSelectedItem();

        cbFilterValues.addItemListener(new ItemAdapter() {
            public void itemSelected(ItemEvent e) {
                filter.value = (ClientFilterValue) cbFilterValues.getSelectedItem();
                filterChanged();
            }
        });

        junctionView = new QueryConditionComboBox(new String[] {ClientResourceBundle.getString("form.queries.and"), ClientResourceBundle.getString("form.queries.or")});
        junctionView.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                filter.junction = junctionView.getSelectedIndex() == 0;
                uiHandlers.conditionChanged();
            }
        });
        centerPanel.add(junctionView);

        add(centerPanel, BorderLayout.CENTER);

        delButton = new ToolbarGridButton(DELETE_ICON_PATH, ClientResourceBundle.getString("form.queries.filter.remove.condition"), new Dimension(getComponentHeight(), getComponentHeight()));
        delButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        uiHandlers.conditionRemoved(filter);
                    }
                });
            }
        });

        filterChanged();
    }


    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(30, super.getPreferredSize().height);
    }

    public void setJunctionVisible(boolean visible) {
        junctionView.setVisible(visible);
    }

    void filterChanged() {
        if (valueView != null) {
            centerPanel.remove(valueView);
        }

        valueView = valueViews.get(filter.value);

        if (valueView != null) {
            centerPanel.add(valueView, Arrays.asList(centerPanel.getComponents()).indexOf(junctionView));
            valueView.propertyChanged(filter.property, filter.columnKey);
        }
        compareView.setModel(new DefaultComboBoxModel(filter.property.baseType.getFilterCompares()));
        compareView.setSelectedItem(null); //чтобы сработал itemStateChanged при автоматическом выборе '='
        compareView.setSelectedItem(filter.getDefaultCompare());

        add(delButton, BorderLayout.EAST);

        validate();

        uiHandlers.conditionChanged();
    }

    // Реализация интерфейса QueryListener
    public void valueChanged() {
        uiHandlers.conditionChanged();
    }

    void requestValueFocus() {
        valueView.requestFocusInWindow();
    }

    public void startEditing(KeyEvent initFilterKeyEvent) {
        valueView.startEditing(initFilterKeyEvent);
    }

    public void setSelectedPropertyDraw(ClientPropertyDraw propertyDraw) {
        if (propertyDraw != null) {
            propertyView.setSelectedItem(propertyDraw);
        }
    }
}
