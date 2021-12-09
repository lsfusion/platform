package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.GridStyle;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class GPropertyTable<T extends GridDataRecord> extends DataGrid<T> implements RenderContext, UpdateContext {

    protected final GFormController form;
    protected final GGroupObject groupObject;

    public GPropertyTable(GFormController iform, GGroupObject groupObject, GridStyle style, boolean noHeaders, boolean noFooters, boolean noScrollers) {
        super(style, noHeaders, noFooters, groupObject.grid.autoSize);

        this.form = iform;
        this.groupObject = groupObject;

        //  Have the enter key work the same as the tab key
        form.addEnterBindings(GBindingMode.ONLY, ((GGridPropertyTable) GPropertyTable.this)::selectNextCellInColumn, this.groupObject);
    }

    public abstract boolean isReadOnly(Cell cell);

    public abstract GPropertyDraw getSelectedProperty();
    public abstract GGroupObjectValue getSelectedColumnKey();

    @Override
    protected void onSelectedChanged(TableCellElement td, int row, int column, boolean selected) {
        GPropertyDraw property = getProperty(row, column);
        if(property != null) {
            if(selected)
                CellRenderer.renderEditSelected(td, property);
            else
                CellRenderer.clearEditSelected(td, property);
        }
    }

    public abstract GPropertyDraw getProperty(int row, int column);
    public abstract GPropertyDraw getProperty(Cell cell);

    @Override
    public boolean isChangeOnSingleClick(Cell cell, boolean rowChanged) {
        GPropertyDraw property = getProperty(cell);
        if(property != null && property.changeOnSingleClick != null)
            return property.changeOnSingleClick;
        return super.isChangeOnSingleClick(cell, rowChanged) || (!rowChanged && GFormController.isLinkEditMode() && property != null && property.hasEditObjectAction);
    }

    public abstract GGroupObjectValue getColumnKey(Cell editCell);

    public abstract GGroupObjectValue getRowKey(Cell editCell);

    public abstract void setValueAt(Cell cell, Object value);

    public abstract Object getValueAt(Cell cell);

    public abstract void pasteData(Cell cell, Element parent, List<List<String>> table);

    @Override
    protected int getRowByKey(Object key) {
        ArrayList<T> rows = getRows();
        for (int i = 0; i < rows.size(); i++) {
            if(rows.get(i).getKey().equals(key))
                return i;
        }
        return -1;
    }

    //    @Override
//    protected void onFocus() {
//        super.onFocus();
//        CopyPasteUtils.setEmptySelection(getSelectedElement());
//    }

    public void onEditEvent(EventHandler handler, boolean isBinding, Cell editCell, Element editCellParent) {
        form.executePropertyEventAction(handler, isBinding, getEditContext(editCell, editCellParent));
    }

    public ExecuteEditContext getEditContext(Cell editCell, Element editCellParent) {
        final GPropertyDraw property = GPropertyTable.this.getProperty(editCell);
        Element editElement = GPropertyTableBuilder.getRenderSizedElement(editCellParent, property, GPropertyTable.this);
        return new ExecuteEditContext() {
            @Override
            public RenderContext getRenderContext() {
                return GPropertyTable.this;
            }

            @Override
            public UpdateContext getUpdateContext() {
                return GPropertyTable.this;
            }

            @Override
            public GPropertyDraw getProperty() {
                return property;
            }

            @Override
            public Element getEditElement() {
                return editElement;
            }

            @Override
            public Element getEditEventElement() {
                return editCellParent;
            }

            @Override
            public Object getValue() {
                return getValueAt(editCell);
            }

            @Override
            public void setValue(Object value) {
                setValueAt(editCell, value);
            }

            @Override
            public GGroupObjectValue getColumnKey() {
                return GPropertyTable.this.getColumnKey(editCell);
            }

            @Override
            public GGroupObjectValue getRowKey() {
                return GPropertyTable.this.getRowKey(editCell);
            }

            @Override
            public boolean isReadOnly() {
                return GPropertyTable.this.isReadOnly(editCell);
            }

            @Override
            public Element getFocusElement() {
                return GPropertyTable.this.getTableDataFocusElement();
            }

            @Override
            public boolean isFocusable() {
                return GPropertyTable.this.isFocusable(editCell);
            }

            @Override
            public void trySetFocus() {
                if (changeSelectedColumn(editCell.getColumnIndex()))
                    getFocusElement().focus();
            }

            @Override
            public boolean isSetLastBlurred() {
                return true;
            }

            @Override
            public Object forceSetFocus() {
                int selectedColumn = getSelectedColumn();
                setSelectedColumn(editCell.getColumnIndex());
                return selectedColumn;
            }

            @Override
            public void restoreSetFocus(Object forceSetFocus) {
                setSelectedColumn((Integer) forceSetFocus);
            }
        };
    }

    @Override
    public boolean isAlwaysSelected() {
        return false;
    }

    @Override
    public boolean globalCaptionIsDrawn() {
        return true;
    }

    @Override
    public Consumer<Object> getCustomRendererValueChangeConsumer() {
        return null;
    }

    @Override
    public boolean isPropertyReadOnly() {
        return false;
    }

    public abstract GFont getFont();

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
