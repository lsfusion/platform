package platform.client.form.queries;

import platform.client.logics.ClientObjectValueLink;
import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.ClientPropertyView;
import platform.client.form.GroupObjectLogicsSupplier;

import javax.swing.*;
import java.util.Vector;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

class ObjectValueLinkView extends ValueLinkView {

    final ClientObjectValueLink valueLink;

    final JComboBox objectView;

    public ObjectValueLinkView(ClientObjectValueLink ivalueLink, GroupObjectLogicsSupplier logicsSupplier) {
        super();

        valueLink = ivalueLink;

        objectView = new QueryConditionComboBox(new Vector<ClientObjectImplementView>(logicsSupplier.getObjects()));

        valueLink.object = (ClientObjectImplementView) objectView.getSelectedItem();

        objectView.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                valueLink.object = (ClientObjectImplementView)e.getItem();
                if (listener != null)
                    listener.valueChanged();
            }
        });

        add(objectView);

    }

    public void propertyChanged(ClientPropertyView property) {
    }
}
