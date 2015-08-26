package lsfusion.client.form.queries;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.GroupObjectLogicsSupplier;
import lsfusion.client.form.ItemAdapter;
import lsfusion.client.logics.*;
import lsfusion.client.logics.filter.ClientPropertyFilter;
import lsfusion.interop.Compare;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class QueryConditionView extends JPanel implements FilterValueListener {
    public interface UIHandlers {
        void conditionChanged();
        void conditionRemoved(ClientPropertyFilter condition);
        void applyQuery();
    }

    private final UIHandlers uiHandlers;

    public static final int PREFERRED_HEIGHT = 18;

    // Icons - загружаем один раз, для экономии
    private static final ImageIcon deleteIcon = new ImageIcon(QueryConditionView.class.getResource("/images/delete.png"));

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

    public QueryConditionView(ClientPropertyFilter ifilter, GroupObjectLogicsSupplier logicsSupplier, UIHandlers iuiHandlers) {

        setAlignmentX(LEFT_ALIGNMENT);

        filter = ifilter;
        uiHandlers = iuiHandlers;

        centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));

        setLayout(new BorderLayout());

        Vector<ClientPropertyDraw> sources = new Vector<ClientPropertyDraw>();
        sources.addAll(logicsSupplier.getGroupObjectProperties());

        propertyView = new QueryConditionComboBox(sources);
        centerPanel.add(propertyView);

        negationView = new JCheckBox(ClientResourceBundle.getString("form.queries.not"));
        negationView.setPreferredSize(new Dimension(negationView.getPreferredSize().width, PREFERRED_HEIGHT));
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

        valueViews = new HashMap<ClientFilterValue, FilterValueView>();

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

        delButton = new ToolbarGridButton(deleteIcon, ClientResourceBundle.getString("form.queries.filter.remove.condition"), new Dimension(PREFERRED_HEIGHT, PREFERRED_HEIGHT));
        delButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                uiHandlers.conditionRemoved(filter);
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
            valueView.propertyChanged(filter.property);
        }
        compareView.setModel(new DefaultComboBoxModel(filter.property.baseType.getFilterCompares()));
        compareView.setSelectedItem(null); //чтобы сработал itemStateChanged при автоматическом выборе '='
        compareView.setSelectedItem(filter.property.baseType.getDefaultCompare());

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
