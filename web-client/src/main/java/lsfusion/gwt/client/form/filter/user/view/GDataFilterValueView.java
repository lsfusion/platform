package lsfusion.gwt.client.form.filter.user.view;

import lsfusion.gwt.client.base.view.ResizableLayoutPanel;
import lsfusion.gwt.shared.form.property.GPropertyDraw;
import lsfusion.gwt.shared.form.object.GGroupObjectValue;
import lsfusion.gwt.shared.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.io.Serializable;

public class GDataFilterValueView extends GFilterValueView {
    private GDataFilterValue filterValue;
    private GTableController logicsSupplier;
    private GDataFilterValueViewTable valueTable;

    private ResizableLayoutPanel tablePanel;

    public GDataFilterValueView(GFilterValueListener listener, GDataFilterValue filterValue, GPropertyDraw property, GTableController logicsSupplier) {
        super(listener);
        this.filterValue = filterValue;
        this.logicsSupplier = logicsSupplier;

        valueTable = new GDataFilterValueViewTable(this, property) {
            @Override
            protected void onFocus() {
                super.onFocus();
                tablePanel.addStyleName("blueBorder");
            }

            @Override
            protected void onBlur() {
                super.onBlur();
                tablePanel.removeStyleName("blueBorder");
            }
        };

        tablePanel = new ResizableLayoutPanel();
        tablePanel.addStyleName("dataFilterValueTablePanel");
        tablePanel.setWidget(valueTable);
        tablePanel.setPixelSize(property.getValueWidth(null), property.getValueHeight(null));

        add(tablePanel);
    }

    @Override
    public void propertyChanged(GPropertyDraw property, GGroupObjectValue columnKey) {
        tablePanel.setPixelSize(property.getValueWidth(null), property.getValueHeight(null));
        valueTable.setProperty(property);
        setValue(logicsSupplier.getSelectedValue(property, columnKey));
    }

    public void valueChanged(Object value) {
        setValue(value);
        listener.valueChanged();
    }

    private void setValue(Object value) {
        filterValue.value = (Serializable) value;
        valueTable.setValue(value);
    }

    public void focusOnValue() {
        valueTable.focusOnValue();
    }

    public void applyFilter() {
    }

    public void startEditing(EditEvent keyEvent) {
        valueTable.startEditing(keyEvent);
    }
}
