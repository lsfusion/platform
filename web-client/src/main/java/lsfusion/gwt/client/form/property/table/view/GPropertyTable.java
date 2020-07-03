package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.GridStyle;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GInputEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

import lsfusion.gwt.client.base.view.grid.cell.Context;

public abstract class GPropertyTable<T extends GridDataRecord> extends DataGrid<T> {

    protected final GFormController form;
    protected final GGroupObject groupObject;

    public GPropertyTable(GFormController iform, GGroupObject iGroupObject, GridStyle style, boolean noHeaders, boolean noFooters, boolean noScrollers) {
        super(style, noHeaders, noFooters, noScrollers);

        this.form = iform;
        this.groupObject = iGroupObject;

        //  Have the enter key work the same as the tab key
        if(groupObject != null) {
            form.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER), null), getEnterBinding(false));
            form.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER, false, false, true), null), getEnterBinding(true));
        }
    }

    protected GFormController.Binding getEnterBinding(boolean shiftPressed) {
        GFormController.Binding binding = new GFormController.Binding(groupObject, -100, null) {
            @Override
            public void pressed(GInputEvent bindingEvent, Event event) {
                ((GGridPropertyTable)GPropertyTable.this).selectNextCellInColumn(!shiftPressed);
            }

            @Override
            public boolean showing() {
                return true;
            }

            @Override
            public boolean enabled() {
                return super.enabled();
            }
        };
        binding.bindGroup = GBindingMode.ONLY;
        return binding;
    }

    public GPropertyDraw getProperty(Column column) {
        return getProperty(new Context(getSelectedRow(), getColumnIndex(column), getSelectedRowValue()));
    }

    public abstract boolean isReadOnly(Context context);

    public abstract GPropertyDraw getSelectedProperty();
    public abstract GGroupObjectValue getSelectedColumnKey();

    public abstract GPropertyDraw getProperty(Context editContext);

    @Override
    public boolean isEditOnSingleClick(Context context) {
        GPropertyDraw property = getProperty(context);
        if(property.editOnSingleClick != null)
            return property.editOnSingleClick;
        return super.isEditOnSingleClick(context);
    }

    public abstract GGroupObjectValue getColumnKey(Context editContext);

    public abstract void setValueAt(Context context, Object value);

    public abstract Object getValueAt(Context context);

    public abstract void pasteData(List<List<String>> table);

//    @Override
//    protected void onFocus() {
//        super.onFocus();
//        CopyPasteUtils.setEmptySelection(getSelectedElement());
//    }

    public void onEditEvent(EventHandler handler, GInputEvent bindingEvent, Context editContext, Element editCellParent) {
        form.executePropertyEventAction(handler, bindingEvent,
                new ExecuteEditContext() {
                    @Override
                    public RenderContext getRenderContext() {
                        return GPropertyTable.this.getRenderContext();
                    }

                    @Override
                    public UpdateContext getUpdateContext() {
                        return GPropertyTable.this.getUpdateContext();
                    }

                    @Override
                    public GPropertyDraw getProperty() {
                        return GPropertyTable.this.getProperty(editContext);
                    }

                    @Override
                    public Element getRenderElement() {
                        return editCellParent;
                    }

                    @Override
                    public Object getValue() {
                        return getValueAt(editContext);
                    }

                    @Override
                    public void setValue(Object value) {
                        setValueAt(editContext, value);
                    }

                    @Override
                    public GGroupObjectValue getColumnKey() {
                        return GPropertyTable.this.getColumnKey(editContext);
                    }

                    @Override
                    public boolean isReadOnly() {
                        return GPropertyTable.this.isReadOnly(editContext);
                    }

                    @Override
                    public Element getFocusElement() {
                        return GPropertyTable.this.getTableDataFocusElement();
                    }

                    @Override
                    public boolean isFocusable() {
                        return GPropertyTable.this.isFocusable(editContext.getColumn());
                    }

                    @Override
                    public void trySetFocus() {
                        if(changeSelectedColumn(editContext.getColumn()))
                            getFocusElement().focus();
                    }

                    @Override
                    public Object forceSetFocus() {
                        int selectedColumn = getSelectedColumn();
                        setSelectedColumn(editContext.getColumn());
                        return selectedColumn;
                    }

                    @Override
                    public void restoreSetFocus(Object forceSetFocus) {
                        setSelectedColumn((Integer)forceSetFocus);
                    }
                }
        );
    }

    public RenderContext getRenderContext() {
        return new RenderContext() {};
    }
    public UpdateContext getUpdateContext() {
        return this::getFont;
    }
    protected abstract GFont getFont();

    protected void setCellHeight(int cellHeight) {
        tableBuilder.setCellHeight(cellHeight);
        setRowHeight(cellHeight + 1); //1px for border
    }

    @Override
    public boolean changeSelectedColumn(int column) {
        form.checkCommitEditing();
        return super.changeSelectedColumn(column);
    }

    @Override
    public void changeSelectedRow(int row) {
        form.checkCommitEditing();
        super.changeSelectedRow(row);
    }
}
