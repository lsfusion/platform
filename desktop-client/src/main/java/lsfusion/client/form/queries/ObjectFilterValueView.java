package lsfusion.client.form.queries;

import lsfusion.client.form.GroupObjectLogicsSupplier;
import lsfusion.client.form.ItemAdapter;
import lsfusion.client.logics.ClientObject;
import lsfusion.client.logics.ClientObjectFilterValue;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Vector;

class ObjectFilterValueView extends FilterValueView {

    private final ClientObjectFilterValue filterValue;

    private final JComboBox objectView;

    public ObjectFilterValueView(FilterValueListener ilistener, ClientObjectFilterValue ifilterValue, GroupObjectLogicsSupplier logicsSupplier) {
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
