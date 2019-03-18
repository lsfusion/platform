package lsfusion.client.form.filter.user;

import lsfusion.client.form.object.table.TableController;
import lsfusion.client.base.ItemAdapter;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.filter.ClientObjectFilterValue;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Vector;

class ObjectFilterValueView extends FilterValueView {

    private final ClientObjectFilterValue filterValue;

    private final JComboBox objectView;

    public ObjectFilterValueView(FilterValueListener ilistener, ClientObjectFilterValue ifilterValue, TableController logicsSupplier) {
        super(ilistener);

        filterValue = ifilterValue;

        objectView = new QueryConditionComboBox(new Vector<>(logicsSupplier.getObjects()));

        filterValue.object = (ClientObject) objectView.getSelectedItem();

        objectView.addItemListener(new ItemAdapter() {
            public void itemSelected(ItemEvent e) {
                filterValue.object = (ClientObject)e.getItem();
                listener.valueChanged();
            }
        });

        add(objectView);

    }
}
