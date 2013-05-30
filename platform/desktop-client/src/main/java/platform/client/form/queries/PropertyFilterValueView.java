package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.ItemAdapter;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.ClientPropertyFilterValue;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Vector;

public class PropertyFilterValueView extends FilterValueView {

    private final ClientPropertyFilterValue filterValue;

    public PropertyFilterValueView(FilterValueListener ilistener, ClientPropertyFilterValue ifilterValue, GroupObjectLogicsSupplier logicsSupplier) {
        super(ilistener);

        filterValue = ifilterValue;

        JComboBox propertyView = new QueryConditionComboBox(new Vector<ClientPropertyDraw>(logicsSupplier.getPropertyDraws()));

        filterValue.property = (ClientPropertyDraw) propertyView.getSelectedItem();

        propertyView.addItemListener(new ItemAdapter() {
            public void itemSelected(ItemEvent e) {
                filterValue.property = (ClientPropertyDraw) e.getItem();
                if (listener != null) {
                    listener.valueChanged();
                }
            }
        });

        add(propertyView);
    }
}
