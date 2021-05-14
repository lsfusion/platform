package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.io.Serializable;

public abstract class GDataFilterValueView extends GFilterValueView {
    private final GDataFilterValue filterValue;
    private final GTableController logicsSupplier;

    public GDataFilterPropertyValue cell;

    public GDataFilterValueView(GDataFilterValue filterValue, GPropertyDraw property, GGroupObjectValue columnKey, GTableController logicsSupplier) {
        this.filterValue = filterValue;
        this.logicsSupplier = logicsSupplier;

        addStyleName("userFilterDataPropertyValue");

        changeProperty(property, columnKey);
    }

    @Override
    public void propertyChanged(GPropertyFilter condition) {
        filterValue.value = (Serializable) logicsSupplier.getSelectedValue(condition.property, condition.columnKey);

        changeProperty(condition.property, condition.columnKey);
    }

    private void changeProperty(GPropertyDraw property, GGroupObjectValue columnKey) {
        cell = new GDataFilterPropertyValue(property, columnKey, logicsSupplier.getForm(), value -> valueChanged(value)) {
            @Override
            protected void onFocus(EventHandler handler) {
                super.onFocus(handler);
                setFocused(true);
            }

            @Override
            protected void onBlur(EventHandler handler) {
                super.onBlur(handler);
                setFocused(false);
            }
        };
        cell.setStatic(this, true);
        cell.updateValue(filterValue.value);
    }

    public void valueChanged(Object value) {
        filterValue.value = (Serializable) value;
    }

    public void focusOnValue() {
        cell.setFocus(true);
    }

    public void startEditing(Event keyEvent) {
        cell.onEditEvent(new EventHandler(keyEvent));
    }
}
