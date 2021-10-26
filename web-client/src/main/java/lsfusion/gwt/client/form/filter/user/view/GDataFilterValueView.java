package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.io.Serializable;

import static lsfusion.gwt.client.form.event.GKeyStroke.isAddUserFilterKeyEvent;
import static lsfusion.gwt.client.form.event.GKeyStroke.isReplaceUserFilterKeyEvent;

public class GDataFilterValueView extends ResizableSimplePanel {
    private final GDataFilterValue filterValue;
    private final GTableController logicsSupplier;

    public GDataFilterPropertyValue cell;

    public GDataFilterValueView(GDataFilterValue filterValue, GTableController logicsSupplier) {
        this.filterValue = filterValue != null ? filterValue : new GDataFilterValue();
        this.logicsSupplier = logicsSupplier;

        addStyleName("userFilterDataPropertyValue");
    }

    public void changeProperty(GPropertyDraw property, GGroupObjectValue columnKey) {
        filterValue.value = null;
        changeProperty(property, columnKey, true);
    }

    public void changeProperty(GPropertyDraw property, GGroupObjectValue columnKey, boolean readSelectedValue) {
        cell = new GDataFilterPropertyValue(property, columnKey, logicsSupplier.getForm(), this::valueChanged, this::editingCancelled);
        
        cell.setStatic(this, true);

        if (readSelectedValue) {
            cell.updateValue(logicsSupplier.getSelectedValue(property, columnKey));
        } else {
            cell.updateValue(filterValue.value);
        }
    }

    public void valueChanged(Object value) {
        filterValue.value = (Serializable) value;
    }
    
    public void editingCancelled() {
        cell.updateValue(filterValue.value);
    }

    public void focusOnValue() {
        cell.setFocus(true);
    }

    public void startEditing(Event keyEvent) {
        if (GwtClientUtils.isShowing(cell)) { // suggest box may appear in (0,0) if filter is already gone (as it's called in scheduleDeferred)
            if (!(cell.getProperty().baseType instanceof GLogicalType)) {
                if (isAddUserFilterKeyEvent(keyEvent) || isReplaceUserFilterKeyEvent(keyEvent)) {
                    cell.startEditing(keyEvent);
                } else {
                    cell.onEditEvent(new EventHandler(keyEvent));
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
    
    public Pair<Integer, Integer> setBaseSize() {
        return cell.setBaseSize(true);
    }
}
