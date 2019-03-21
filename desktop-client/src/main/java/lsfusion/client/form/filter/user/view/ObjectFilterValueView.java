package lsfusion.client.form.filter.user.view;

import lsfusion.client.base.view.ItemAdapter;
import lsfusion.client.form.filter.user.ClientObjectFilterValue;
import lsfusion.client.form.filter.user.FilterValueListener;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.object.table.controller.TableController;

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
