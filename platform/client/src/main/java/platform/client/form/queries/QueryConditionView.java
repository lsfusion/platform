package platform.client.form.queries;

import platform.client.ClientButton;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.*;
import platform.client.logics.classes.ClientInsensitiveStringClass;
import platform.client.logics.classes.ClientStringClass;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.interop.Compare;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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
    private JComboBox propertyView;
    private JComboBox compareView;

    public QueryConditionView(ClientPropertyFilter ifilter, GroupObjectLogicsSupplier logicsSupplier) {

        filter = ifilter;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        Vector<ClientPropertyDraw> sources = new Vector<ClientPropertyDraw>();
        sources.addAll(logicsSupplier.getGroupObjectProperties());

        propertyView = new QueryConditionComboBox(sources);
        add(propertyView);

        compareView = new QueryConditionComboBox(Compare.values());
        add(compareView);

        if (filter.property != null) {
            setSelectedPropertyDraw(filter.property);
        }
        filter.compare = (Compare) compareView.getSelectedItem();

        propertyView.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ie) {
                if (ie.getStateChange() == ItemEvent.SELECTED) {
                    filter.property = (ClientPropertyDraw) ie.getItem();
                    filterChanged();
                }
            }
        });

        compareView.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    filter.compare = (Compare) e.getItem();
                    conditionChanged();
                }
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

        ClientValueLink[] classes = new ClientValueLink[]{userValue, objectValue, propertyValue};
        classValueLinkView = new QueryConditionComboBox(classes);
        add(classValueLinkView);

        filter.value = (ClientValueLink) classValueLinkView.getSelectedItem();

        classValueLinkView.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ie) {
                if (ie.getStateChange() == ItemEvent.SELECTED) {
                    filter.value = (ClientValueLink) classValueLinkView.getSelectedItem();
                    filterChanged();
                }
            }
        });

        delButton = new ClientButton(deleteIcon);
        delButton.setFocusable(false);
        delButton.setPreferredSize(new Dimension(PREFERRED_HEIGHT, PREFERRED_HEIGHT));
        delButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                conditionRemoved(filter);
            }
        });

        filterChanged();

    }

    void filterChanged() {

        if (valueView != null) {
            remove(valueView);
        }

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

    void requestValueFocus() {
        valueView.requestFocusInWindow();
    }

    public void startEditing() {
        valueView.startEditing();
    }

    public static Map<Class, Compare> defaultCompares = new HashMap<Class, Compare>(){{
        put(ClientStringClass.class, Compare.START_WITH);
        put(ClientInsensitiveStringClass.class, Compare.START_WITH);
    }};

    public void setSelectedPropertyDraw(ClientPropertyDraw propertyDraw) {
        if (propertyDraw != null) {
            propertyView.setSelectedItem(propertyDraw);
            Compare compareToSet = defaultCompares.get(propertyDraw.baseType.getClass());
            if (compareToSet == null) {
                compareToSet = Compare.EQUALS;
            }
            compareView.setSelectedItem(compareToSet);
        }
    }
}
