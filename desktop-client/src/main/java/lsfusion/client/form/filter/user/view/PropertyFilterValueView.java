package lsfusion.client.form.filter.user.view;

import lsfusion.client.form.filter.user.FilterValueListener;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.base.ItemAdapter;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.filter.user.ClientPropertyFilterValue;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Vector;

public class PropertyFilterValueView extends FilterValueView {

    private final ClientPropertyFilterValue filterValue;

    public PropertyFilterValueView(FilterValueListener ilistener, ClientPropertyFilterValue ifilterValue, TableController logicsSupplier) {
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
