package lsfusion.client.form.filter.user.view;

import lsfusion.client.base.view.ItemAdapter;
import lsfusion.client.classes.ClientActionClass;
import lsfusion.client.form.filter.user.ClientPropertyFilterValue;
import lsfusion.client.form.filter.user.FilterValueListener;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.*;

public class PropertyFilterValueView extends FilterValueView {

    private final ClientPropertyFilterValue filterValue;

    public PropertyFilterValueView(FilterValueListener ilistener, ClientPropertyFilterValue ifilterValue, TableController logicsSupplier) {
        super(ilistener);

        filterValue = ifilterValue;

        LinkedHashMap<String, ClientPropertyDraw> properties = new LinkedHashMap<>();

        List<ClientPropertyDraw> groupObjectProperties = logicsSupplier.getGroupObjectProperties();
        for(ClientPropertyDraw property : groupObjectProperties) {
            if(!(property.baseType instanceof ClientActionClass)) {
                properties.put(property.toString(), property);
            }
        }

        for(ClientPropertyDraw property : logicsSupplier.getPropertyDraws()) {
            if(!(property.baseType instanceof ClientActionClass) && !groupObjectProperties.contains(property)) {
                properties.put(property.getFilterCaption(logicsSupplier.getGroupObject()), property);
            }
        }

        JComboBox propertyView = new QueryConditionComboBox(new Vector<>(properties.keySet()));

        filterValue.property = properties.get(propertyView.getSelectedItem());

        propertyView.addItemListener(new ItemAdapter() {
            public void itemSelected(ItemEvent e) {
                filterValue.property = properties.get(e.getItem());
                if (listener != null) {
                    listener.valueChanged();
                }
            }
        });

        add(propertyView);
    }
}
