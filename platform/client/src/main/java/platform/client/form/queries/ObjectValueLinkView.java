package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientObject;
import platform.client.logics.ClientObjectValueLink;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

class ObjectValueLinkView extends ValueLinkView {

    private final ClientObjectValueLink valueLink;

    private final JComboBox objectView;

    public ObjectValueLinkView(ClientObjectValueLink ivalueLink, GroupObjectLogicsSupplier logicsSupplier) {
        super();

        valueLink = ivalueLink;

        objectView = new QueryConditionComboBox(new Vector<ClientObject>(logicsSupplier.getObjects()));

        valueLink.object = (ClientObject) objectView.getSelectedItem();

        objectView.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                valueLink.object = (ClientObject)e.getItem();
                if (listener != null)
                    listener.valueChanged();
            }
        });

        add(objectView);

    }

    public void propertyChanged(ClientPropertyDraw property) {
    }
}
