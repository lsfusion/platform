package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GDataFilterValue;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import static lsfusion.gwt.client.form.event.GKeyStroke.isAddUserFilterKeyEvent;
import static lsfusion.gwt.client.form.event.GKeyStroke.isReplaceUserFilterKeyEvent;

public class GDataFilterValueView extends SizedFlexPanel {
    private final GDataFilterValue filterValue;
    private final GFormController form;
    private final GTableController rows;

    public GDataFilterPropertyValue cell;

    public GDataFilterValueView(GDataFilterValue filterValue, GFormController form, GTableController rows) {
        this.filterValue = filterValue != null ? filterValue : new GDataFilterValue();
        this.form = form;
        this.rows = rows;
    }

    private SizedWidget sizedView; // needed only to remove previous widget when changing properties

    // the value cell is a property value like any other, so it is registered where they all are - a list that lives as
    // long as the page - and has to unregister when it is dropped: when the property changes and it is rebuilt, and
    // when the condition it belongs to is removed. Otherwise it keeps the form it was drawn for
    public void destroy() {
        if(cell != null) {
            form.removeBindings(cell); // the enter binding this cell was given when it was built
            cell.destroy();
        }
    }

    // its editor, if open, cancelled - before its row is bound to another entry or taken away
    // (GPlatformFilterRowsController): an editor left open would commit into what it no longer shows
    public void cancelEditing() {
        if (cell != null)
            form.cancelEditing(cell, CancelReason.HIDE);
    }

    public void changeProperty(GPropertyFilter condition, boolean readSelectedValue) {
        if(sizedView != null) {
            removeSized(sizedView.widget);
            destroy(); // the cell being replaced registered itself in a list that lives as long as the page
        }

        cell = new GDataFilterPropertyValue(condition, form, this::valueChanged, this::editingCancelled);
        addEnterBinding(cell, condition.groupObject);

        sizedView = cell.getSizedWidget(true); // true is needed for correct calculation of input field width in filters
        GwtClientUtils.addClassName(sizedView.widget, "filter-data-property-value");
        // we're drawing the border ourselves (so isInputRemoveAllPMV is true),
        // otherwise there would be a problem, that classes for the property are set assuming that it is a grid, and there can be problems when setting them in the filter (because for example form-control will be missing in this case)
        GwtClientUtils.addClassName(sizedView.widget, "form-control");
        sizedView.addFill(this);

        if (readSelectedValue && !condition.column.property.differentValue)
            filterValue.value = readSelectedValue(condition);

        cell.updateValue(filterValue.value);
    }

    // Enter in the cell alone - the binding's widget says only whether it is shown, and the group's other panel cells
    // give the form their Enter too -: bound with each cell built, taken back with it (destroy)
    private void addEnterBinding(GDataFilterPropertyValue cell, GGroupObject group) {
        GKeyInputEvent enter = new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER));
        form.addBinding(event -> enter.isEvent(event) && cell.getElement().isOrHasChild(Element.as(event.getEventTarget())),
                new GBindingEnv(null, GBindingMode.ALL, null, null, GBindingMode.ONLY, null, null, GBindingMode.ONLY, null),
                null, event -> {
                    enterPressed();
                    GwtClientUtils.stopPropagation(event);
                },
                cell, group);
    }
    // ... what it does: the row applies the conditions (GFilterConditionView)
    public void enterPressed() {
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
        return PValue.escapeSeparator(rows.getSelectedValue(condition.column.property, condition.column.columnKey), condition.compare);
    } 
    
    public void setValue(PValue value) {
        filterValue.value = value;
        cell.updateValue(value);
    }
    
    public void startEditing(Event keyEvent) {
        if (GwtClientUtils.isShowing(cell) && !form.isEditing()) { // suggest box may appear in (0,0) if filter is already gone (as it's called in scheduleDeferred)
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
