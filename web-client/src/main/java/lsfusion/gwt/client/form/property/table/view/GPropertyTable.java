package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.GridStyle;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.*;
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
            addEnterBinding(false);
            addEnterBinding(true);
        }
    }

    private void addEnterBinding(boolean shiftPressed) {
        form.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER, false, false, shiftPressed)),
                new GBindingEnv(-100, null, GBindingMode.ONLY, GBindingMode.NO, null),  // bindEditing - NO, because we don't want for example when editing text in grid to catch enter
                event -> ((GGridPropertyTable) GPropertyTable.this).selectNextCellInColumn(!shiftPressed),
                null,
                groupObject);
    }

    public abstract boolean isReadOnly(Context context);

    public abstract GPropertyDraw getSelectedProperty();
    public abstract GGroupObjectValue getSelectedColumnKey();

    public abstract GPropertyDraw getProperty(Context editContext);

    @Override
    public boolean isEditOnSingleClick(Context context) {
        GPropertyDraw property = getProperty(context);
        if(property != null && property.editOnSingleClick != null)
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

    public void onEditEvent(EventHandler handler, boolean isBinding, Context editContext, Element editCellParent) {
        form.executePropertyEventAction(handler, isBinding,
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
                        return GPropertyTable.this.isFocusable(editContext);
                    }

                    @Override
                    public void trySetFocus() {
                        if(changeSelectedColumn(editContext.getColumnIndex()))
                            getFocusElement().focus();
                    }

                    @Override
                    public Object forceSetFocus() {
                        int selectedColumn = getSelectedColumn();
                        setSelectedColumn(editContext.getColumnIndex());
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
        return new RenderContext() {
            @Override
            public Integer getStaticHeight() {
                return tableBuilder.getCellHeight();
            }

            @Override
            public GFont getFont() {
                return GPropertyTable.this.getFont();
            }
        };
    }
    public UpdateContext getUpdateContext() {
        return new UpdateContext() {
            @Override
            public boolean isStaticHeight() {
                return true;
            }
        };
    }
    protected abstract GFont getFont();

    protected void setCellHeight(int cellHeight) {
        setRowHeight(cellHeight + 1); //1px for border
        if(tableBuilder.setCellHeight(cellHeight))
            refreshColumnsAndRedraw();
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
