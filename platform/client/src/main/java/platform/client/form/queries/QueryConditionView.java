package platform.client.form.queries;

import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.logics.*;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.interop.Compare;
import platform.base.Pair;

import javax.swing.*;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;
import java.awt.*;
import java.awt.event.*;

abstract class QueryConditionView extends JPanel implements ValueLinkListener {

    public static final int PREFERRED_HEIGHT = 18;

    // Icons - загружаем один раз, для экономии
    @SuppressWarnings({"FieldCanBeLocal"})
    private final ImageIcon deleteIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/delete.gif"));

    private final ClientPropertyFilter filter;

    private final JComboBox classValueLinkView;

    private ValueLinkView valueView;
    private final Map<ClientValueLink, ValueLinkView> valueViews;

    private final JButton delButton;

    public QueryConditionView(ClientPropertyFilter ifilter, GroupObjectLogicsSupplier logicsSupplier) {

        filter = ifilter;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        Vector<ClientPropertyView> sources = new Vector<ClientPropertyView>();
        sources.addAll(logicsSupplier.getGroupObjectProperties());

        JComboBox propertyView = new QueryConditionComboBox(sources);
        add(propertyView);

        if (logicsSupplier.getDefaultProperty() != null)
            propertyView.setSelectedItem(logicsSupplier.getDefaultProperty());

        filter.property = (ClientPropertyView) propertyView.getSelectedItem();

        propertyView.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ie) {

                filter.property = (ClientPropertyView)ie.getItem();
                filterChanged();
            }
        });

        @SuppressWarnings({"unchecked"})
        Pair<String,Compare>[] comparisons = new Pair[] {new Pair("=", Compare.EQUALS), new Pair(">", Compare.GREATER), new Pair("<", Compare.LESS),
                                                         new Pair(">=", Compare.GREATER_EQUALS), new Pair("<=", Compare.LESS_EQUALS), new Pair("<>", Compare.NOT_EQUALS)};

        JComboBox compareView = new QueryConditionComboBox(comparisons);
        add(compareView);

        filter.compare = ((Pair<String,Compare>) compareView.getSelectedItem()).second;

        compareView.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                filter.compare = ((Pair<String,Compare>)e.getItem()).second;
                conditionChanged();
            }
        });

        valueViews = new HashMap<ClientValueLink, ValueLinkView>();

        ClientUserValueLink userValue = new ClientUserValueLink();
        UserValueLinkView userView = new UserValueLinkView(userValue, filter.property, logicsSupplier);
        userView.setListener(this);
        valueViews.put(userValue, userView);

        ClientObjectValueLink objectValue = new ClientObjectValueLink();
        ObjectValueLinkView objectView = new ObjectValueLinkView(objectValue, logicsSupplier);
        objectView.setListener(this);
        valueViews.put(objectValue, objectView);

        ClientPropertyValueLink propertyValue = new ClientPropertyValueLink();
        PropertyValueLinkView propertyValueView = new PropertyValueLinkView(propertyValue, logicsSupplier);
        propertyValueView.setListener(this);
        valueViews.put(propertyValue, propertyValueView);

        ClientValueLink[] classes = new ClientValueLink[] {userValue, objectValue, propertyValue};
        classValueLinkView = new QueryConditionComboBox(classes);
        add(classValueLinkView);

        filter.value = (ClientValueLink)classValueLinkView.getSelectedItem();

        classValueLinkView.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ie) {
                filter.value = (ClientValueLink)classValueLinkView.getSelectedItem();
                filterChanged();
            }
        });

        delButton = new JButton(deleteIcon);
        delButton.setFocusable(false);
        delButton.setPreferredSize(new Dimension(PREFERRED_HEIGHT, PREFERRED_HEIGHT));
        delButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                conditionRemoved(filter);
            }
        });

        filterChanged();

    }

    void requestValueFocus() {
        valueView.requestFocusInWindow();
    }

    public void filterChanged() {

        if (valueView != null)
            remove(valueView);

        valueView = valueViews.get(filter.value);

        if (valueView != null) {
            add(valueView);
            valueView.propertyChanged(filter.property);
        }

        add(delButton);

        validate();

        conditionChanged();
    }

    protected abstract void conditionChanged();
    protected abstract void conditionRemoved(ClientPropertyFilter condition);

    void stopEditing() {
        valueView.stopEditing();
    }

    // Реализация интерфейса QueryListener
    public void valueChanged() {
        conditionChanged();
    }
}
