package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
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

public class GDataFilterValueView extends FlexPanel {
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
            remove(sizedView.widget);

        cell = new GDataFilterPropertyValue(condition, logicsSupplier.getForm(), this::valueChanged, this::editingCancelled);

        GPropertyDraw property = condition.property;
        // pretty similar to PropertyPanelRenderer (except that we don't need needCorners)
        // there is an extra container of course (but the same problem is for PropertyPanelRenderer)
        ResizableComplexPanel valuePanel = null;
        if(!property.autoSize)
            valuePanel = new ResizableComplexPanel();
        sizedView = cell.setSized(valuePanel);
        sizedView.widget.addStyleName("userFilterDataPropertyValue");
        sizedView.addFill(this);

        Object value;
        if (readSelectedValue) {
            value = logicsSupplier.getSelectedValue(property, condition.columnKey);
        } else {
            value = filterValue.value;
        }
        cell.update(value);
    }

    public void valueChanged(Object value) {
        filterValue.value = (Serializable) value;
    }
    
    public void changeCompare(GCompare compare) {
        cell.changeInputList(compare);
    }
    
    public void editingCancelled(CancelReason cancelReason) {
        cell.update(filterValue.value);
    }

    public void focusOnValue() {
        cell.setFocus(true);
    }

    public void startEditing(Event keyEvent) {
        if (GwtClientUtils.isShowing(cell) && !logicsSupplier.getForm().isEditing()) { // suggest box may appear in (0,0) if filter is already gone (as it's called in scheduleDeferred)
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
}
