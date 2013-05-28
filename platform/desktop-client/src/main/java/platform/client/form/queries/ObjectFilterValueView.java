package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.ItemAdapter;
import platform.client.logics.ClientObject;
import platform.client.logics.ClientObjectFilterValue;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Vector;

class ObjectFilterValueView extends FilterValueView {

    private final ClientObjectFilterValue filterValue;

    private final JComboBox objectView;

    public ObjectFilterValueView(FilterValueListener ilistener, ClientObjectFilterValue ifilterValue, GroupObjectLogicsSupplier logicsSupplier) {
        super(ilistener);

        filterValue = ifilterValue;

        objectView = new QueryConditionComboBox(new Vector<ClientObject>(logicsSupplier.getObjects()));

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
