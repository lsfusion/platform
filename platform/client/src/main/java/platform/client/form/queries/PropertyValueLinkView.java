package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientPropertyValueLink;
import platform.client.logics.ClientPropertyView;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

class PropertyValueLinkView extends ValueLinkView {

    private final ClientPropertyValueLink valueLink;

    public PropertyValueLinkView(ClientPropertyValueLink ivalueLink, GroupObjectLogicsSupplier logicsSupplier) {
        super();

        valueLink = ivalueLink;

        JComboBox propertyView = new QueryConditionComboBox(new Vector<ClientPropertyView>(logicsSupplier.getProperties()));

        valueLink.property = (ClientPropertyView) propertyView.getSelectedItem();

        propertyView.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {

                valueLink.property = (ClientPropertyView)e.getItem();
                if (listener != null)
                    listener.valueChanged();
            }
        });

        add(propertyView);
    }

    public void propertyChanged(ClientPropertyView property) {
    }

}
