package lsfusion.gwt.client.form.filter.user.view;

import lsfusion.gwt.client.base.view.ResizableLayoutPanel;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;

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
                tablePanel.addStyleName("dataFilterValueTablePanelFocused");
            }

            @Override
            protected void onBlur() {
                super.onBlur();
                tablePanel.removeStyleName("dataFilterValueTablePanelFocused");
            }
        };

        tablePanel = new ResizableLayoutPanel();
        tablePanel.addStyleName("dataFilterValueTablePanel");
        tablePanel.setWidget(valueTable);
        tablePanel.setPixelSize(property.getValueWidth(null), property.getValueHeight(null));

        add(tablePanel);
    }

    @Override
    public void propertySet(GPropertyFilter condition) {
        setProperty(condition);
        valueTable.setValue(filterValue.value);
    }

    @Override
    public void propertyChanged(GPropertyFilter condition) {
        setProperty(condition);
        setValue(logicsSupplier.getSelectedValue(condition.property, condition.columnKey));
    }

    private void setProperty(GPropertyFilter condition) {
        tablePanel.setPixelSize(condition.property.getValueWidth(null), condition.property.getValueHeight(null));
        valueTable.setProperty(condition.property);
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
