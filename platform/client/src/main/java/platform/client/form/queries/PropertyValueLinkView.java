package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.ItemAdapter;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.ClientPropertyValueLink;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Vector;

public class PropertyValueLinkView extends ValueLinkView {

    private final ClientPropertyValueLink valueLink;

    public PropertyValueLinkView(ClientPropertyValueLink ivalueLink, GroupObjectLogicsSupplier logicsSupplier) {
        super();

        valueLink = ivalueLink;

        JComboBox propertyView = new QueryConditionComboBox(new Vector<ClientPropertyDraw>(logicsSupplier.getPropertyDraws()));

        valueLink.property = (ClientPropertyDraw) propertyView.getSelectedItem();

        propertyView.addItemListener(new ItemAdapter() {
            public void itemSelected(ItemEvent e) {
                valueLink.property = (ClientPropertyDraw) e.getItem();
                if (listener != null) {
                    listener.valueChanged();
                }
            }
        });

        add(propertyView);
    }

    public void propertyChanged(ClientPropertyDraw property) {
    }
}
