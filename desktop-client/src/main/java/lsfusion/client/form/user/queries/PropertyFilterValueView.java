package lsfusion.client.form.user.queries;

import lsfusion.client.form.object.GroupObjectLogicsSupplier;
import lsfusion.client.form.user.ItemAdapter;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.form.filter.ClientPropertyFilterValue;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Vector;

public class PropertyFilterValueView extends FilterValueView {

    private final ClientPropertyFilterValue filterValue;

    public PropertyFilterValueView(FilterValueListener ilistener, ClientPropertyFilterValue ifilterValue, GroupObjectLogicsSupplier logicsSupplier) {
        super(ilistener);

        filterValue = ifilterValue;

        JComboBox propertyView = new QueryConditionComboBox(new Vector<>(logicsSupplier.getPropertyDraws()));

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
