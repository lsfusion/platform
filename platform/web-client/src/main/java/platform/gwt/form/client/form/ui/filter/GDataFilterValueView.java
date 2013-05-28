package platform.gwt.form.client.form.ui.filter;

import platform.gwt.base.client.ui.ResizableLayoutPanel;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GDataFilterValue;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;

import java.io.Serializable;

public class GDataFilterValueView extends GFilterValueView {
    private GDataFilterValue filterValue;
    private GGroupObjectLogicsSupplier logicsSupplier;
    private GDataFilterValueViewTable valueTable;

    private ResizableLayoutPanel tablePanel;

    public GDataFilterValueView(GFilterValueListener listener, GDataFilterValue filterValue, GPropertyDraw property, GGroupObjectLogicsSupplier logicsSupplier) {
        super(listener);
        this.filterValue = filterValue;
        this.logicsSupplier = logicsSupplier;

        valueTable = new GDataFilterValueViewTable(this, property);

        tablePanel = new ResizableLayoutPanel();
        tablePanel.addStyleName("dataFilterValueTablePanel");
        tablePanel.setWidget(valueTable);
        tablePanel.setPixelSize(property.getPreferredPixelWidth(), property.getMinimumPixelHeight());

        add(tablePanel);
    }

    @Override
    public void propertyChanged(GPropertyDraw property) {
        tablePanel.setPixelSize(property.getPreferredPixelWidth(), property.getMinimumPixelHeight());
        valueTable.setProperty(property);
        setValue(logicsSupplier.getSelectedValue(property));
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
