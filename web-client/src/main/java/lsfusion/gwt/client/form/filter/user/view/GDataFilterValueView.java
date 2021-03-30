package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.io.Serializable;

public class GDataFilterValueView extends GFilterValueView {
    private final GDataFilterValue filterValue;
    private final GTableController logicsSupplier;

    public GDataFilterPropertyValue cell;

    public GDataFilterValueView(GFilterValueListener listener, GDataFilterValue filterValue, GPropertyDraw property, GTableController logicsSupplier) {
        super(listener);
        this.filterValue = filterValue;
        this.logicsSupplier = logicsSupplier;

        addStyleName("userFilterDataPropertyValue");
        
        cell = new GDataFilterPropertyValue(property, logicsSupplier.getForm(), value -> valueChanged(value));
        cell.setStatic(this, true);
    }

    @Override
    public void propertySet(GPropertyFilter condition) {
        changeProperty(condition.property);
    }

    @Override
    public void propertyChanged(GPropertyFilter condition) {
        filterValue.value = (Serializable) logicsSupplier.getSelectedValue(condition.property, condition.columnKey);

        changeProperty(condition.property);
    }

    private void changeProperty(GPropertyDraw property) {
        cell.changeProperty(property);
        cell.updateValue(filterValue.value);
    }

    public void valueChanged(Object value) {
        filterValue.value = (Serializable) value;
        listener.valueChanged();
    }

    public void focusOnValue() {
        cell.setFocus(true);
    }

    public void startEditing(Event keyEvent) {
        cell.onEditEvent(new EventHandler(keyEvent));
    }
}
