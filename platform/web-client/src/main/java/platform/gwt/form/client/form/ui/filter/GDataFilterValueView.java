package platform.gwt.form.client.form.ui.filter;

import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GDataFilterValue;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;

import java.io.Serializable;

public class GDataFilterValueView extends GFilterValueView {
    private GDataFilterValue filterValue;
    private GGroupObjectLogicsSupplier logicsSupplier;
    private GDataFilterValueViewTable valueTable;

    private ResizeLayoutPanel tablePanel;

    public GDataFilterValueView(GFilterValueListener listener, GDataFilterValue filterValue, GPropertyDraw property, GGroupObjectLogicsSupplier logicsSupplier) {
        super(listener);
        this.filterValue = filterValue;
        this.logicsSupplier = logicsSupplier;

        valueTable = new GDataFilterValueViewTable(this, property);

        tablePanel = new ResizeLayoutPanel();
        tablePanel.addStyleName("dataFilterValueTablePanel");
        tablePanel.setWidget(valueTable);
        tablePanel.setPixelSize(property.getPreferredPixelWidth(), 16);

        add(tablePanel);
    }

    @Override
    public void propertyChanged(GPropertyDraw property) {
        tablePanel.setPixelSize(property.getPreferredPixelWidth(), 16);
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
}
