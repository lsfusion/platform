package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

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

    private SizedWidget sizedView; // needed only to remove previous widget when changing properties

    public void changeProperty(GPropertyFilter condition, boolean readSelectedValue) {
        if(sizedView != null)
            removeSized(sizedView.widget);

        cell = new GDataFilterPropertyValue(condition, logicsSupplier.getForm(), this::valueChanged, this::editingCancelled);

        sizedView = cell.getSizedWidget(true); // true is needed for correct calculation of input field width in filters
        GwtClientUtils.addClassName(sizedView.widget, "filter-data-property-value");
        // we're drawing the border ourselves (so isInputRemoveAllPMV is true),
        // otherwise there would be a problem, that classes for the property are set assuming that it is a grid, and there can be problems when setting them in the filter (because for example form-control will be missing in this case)
        GwtClientUtils.addClassName(sizedView.widget, "form-control");
        sizedView.addFill(this);

        if (readSelectedValue && !condition.property.differentValue)
            filterValue.value = readSelectedValue(condition);

        cell.updateValue(filterValue.value);
    }

    public void valueChanged(PValue value) {
        filterValue.value = value;
    }
    
    public void changeCompare(GCompare compare) {
        cell.changeInputList(compare);
    }
    
    public void editingCancelled(CancelReason cancelReason) {
        // in theory not needed, because will be updated in finishEditing
//        cell.updateValue(filterValue.value);
    }

    public void onAdd(boolean focus) {
        if (focus) {
            cell.focus(FocusUtils.Reason.NEWFILTER);
        }
    }

    public void focusOnValue() {
        cell.focus(FocusUtils.Reason.OTHER);
    }

    private PValue readSelectedValue(GPropertyFilter condition) {
        return PValue.escapeSeparator(logicsSupplier.getSelectedValue(condition.property, condition.columnKey), condition.compare);
    } 
    
    public void putSelectedValue(GPropertyFilter condition) {
        filterValue.value = readSelectedValue(condition);
        cell.updateValue(filterValue.value);
    }
    
    public void setValue(PValue value) {
        filterValue.value = value;
        cell.updateValue(value);
    }
    
    public void startEditing(Event keyEvent) {
        if (GwtClientUtils.isShowing(cell) && !logicsSupplier.getForm().isEditing()) { // suggest box may appear in (0,0) if filter is already gone (as it's called in scheduleDeferred)
            GPropertyDraw gPropertyDraw = cell.getProperty();
            if (!(gPropertyDraw.getRenderType(RendererType.FILTER) instanceof GLogicalType)) {
                cell.onEditEvent(new EventHandler(keyEvent), isAddUserFilterKeyEvent(keyEvent) || isReplaceUserFilterKeyEvent(keyEvent));
//                EventHandler handler = new EventHandler(keyEvent);
//                if (isAddUserFilterKeyEvent(keyEvent) || isReplaceUserFilterKeyEvent(keyEvent)) {
//                    cell.startEditing(handler);
//                } else {
//                    cell.onEditEvent(handler);
//                }
            } else {
                // to be able to apply on Enter
                filterValue.value = cell.getValue();
            }
        }
    }

    public void setApplied(boolean applied) {
        cell.setApplied(applied);
    }
}
