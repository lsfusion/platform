package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;

import java.io.Serializable;

import static lsfusion.gwt.client.form.event.GKeyStroke.isAddUserFilterKeyEvent;
import static lsfusion.gwt.client.form.event.GKeyStroke.isReplaceUserFilterKeyEvent;

public class GDataFilterValueView extends SizedFlexPanel {
    private final GDataFilterValue filterValue;
    private final GTableController logicsSupplier;

    public GDataFilterPropertyValue cell;

    public GDataFilterValueView(GDataFilterValue filterValue, GTableController logicsSupplier) {
        this.filterValue = filterValue != null ? filterValue : new GDataFilterValue();
        this.logicsSupplier = logicsSupplier;
    }

    public void changeProperty(GPropertyFilter condition) {
        filterValue.value = null;
        changeProperty(condition, true);
    }

    private SizedWidget sizedView; // needed only to remove previous widget when changing properties

    public void changeProperty(GPropertyFilter condition, boolean readSelectedValue) {
        if(sizedView != null)
            removeSized(sizedView.widget);

        cell = new GDataFilterPropertyValue(condition, logicsSupplier.getForm(), this::valueChanged, this::editingCancelled);

        GPropertyDraw property = condition.property;
        sizedView = cell.getSizedWidget();
        sizedView.widget.addStyleName("userFilterDataPropertyValue");
        sizedView.addFill(this);

        Object value;
        if (readSelectedValue) {
            value = GwtClientUtils.escapeSeparator(logicsSupplier.getSelectedValue(property, condition.columnKey), condition.compare);
        } else {
            value = filterValue.value;
        }
        cell.updateValue(value);
    }

    public void valueChanged(Object value) {
        filterValue.value = (Serializable) value;
    }
    
    public void changeCompare(GCompare compare) {
        cell.changeInputList(compare);
    }
    
    public void editingCancelled(CancelReason cancelReason) {
        cell.updateValue(filterValue.value);
    }

    public void onAdd() {
        cell.focus(FocusUtils.Reason.NEWFILTER);
    }

    public void startEditing(Event keyEvent) {
        if (GwtClientUtils.isShowing(cell) && !logicsSupplier.getForm().isEditing()) { // suggest box may appear in (0,0) if filter is already gone (as it's called in scheduleDeferred)
            if (!(cell.getProperty().baseType instanceof GLogicalType)) {
                EventHandler handler = new EventHandler(keyEvent);
                if (isAddUserFilterKeyEvent(keyEvent) || isReplaceUserFilterKeyEvent(keyEvent)) {
                    cell.startEditing(handler);
                } else {
                    cell.onEditEvent(handler);
                }
            } else {
                // to be able to apply on Enter
                filterValue.value = (Serializable) cell.getValue();
            }
        }
    }

    public void setApplied(boolean applied) {
        cell.setApplied(applied);
    }
}
