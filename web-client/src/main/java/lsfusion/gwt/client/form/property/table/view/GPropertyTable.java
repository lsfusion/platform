package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.GridStyle;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.ArrayList;
import java.util.List;

public abstract class GPropertyTable<T extends GridDataRecord> extends DataGrid<T> {

    protected final GFormController form;
    protected final GGroupObject groupObject;

    public GPropertyTable(GFormController iform, GGroupObject groupObject, TableContainer tableContainer, GridStyle style, boolean noHeaders, boolean noFooters, boolean noScrollers) {
        super(tableContainer, style, noHeaders, noFooters);

        this.form = iform;
        this.groupObject = groupObject;

        //  Have the enter key work the same as the tab key
        form.addEnterBindings(GBindingMode.ONLY, ((GGridPropertyTable) GPropertyTable.this)::selectNextCellInColumn, this.groupObject);
    }

    public abstract boolean isReadOnly(Cell cell);

    public abstract GPropertyDraw getSelectedProperty();
    public abstract GGroupObjectValue getSelectedColumnKey();

    public abstract GPropertyDraw getProperty(Cell cell);
    public abstract GGridPropertyTable<T>.GridPropertyColumn getGridColumn(int column);

    @Override
    public boolean isChangeOnSingleClick(Cell cell, Event event, boolean rowChanged) {
        GPropertyDraw property = getProperty(cell);
        if(property != null && property.changeOnSingleClick != null)
            return property.changeOnSingleClick;
        return super.isChangeOnSingleClick(cell, event, rowChanged) ||
                (!rowChanged && GFormController.isLinkMode() && property != null && property.hasEditObjectAction) ||
                (GMouseStroke.isChangeEvent(event) && GEditBindingMap.getToolbarAction(event) != null);
    }

    public abstract GGroupObjectValue getColumnKey(Cell editCell);

    public abstract GGroupObjectValue getRowKey(Cell editCell);

    public abstract void setValueAt(Cell cell, Object value);

    public abstract void setLoadingAt(Cell cell);

    public abstract void pasteData(Cell cell, TableCellElement parent, List<List<String>> table);

    @Override
    protected int findRowByKey(Object key, int expandingIndex) {
        ArrayList<T> rows = getRows();
        for (int i = 0; i < rows.size(); i++) {
            T row = rows.get(i);
            if(row.getKey().equals(key) && row.getExpandingIndex() == expandingIndex)
                return i;
        }
        return -1;
    }

    //    @Override
//    protected void onFocus() {
//        super.onFocus();
//        CopyPasteUtils.setEmptySelection(getSelectedElement());
//    }

    public void onEditEvent(EventHandler handler, boolean isBinding, Cell editCell, TableCellElement editCellParent) {
        form.executePropertyEventAction(handler, isBinding, getEditContext(editCell, editCellParent));
    }

    protected abstract RenderContext getRenderContext(Cell cell, TableCellElement cellElement, GPropertyDraw property, GGridPropertyTable<T>.GridPropertyColumn column);

    protected abstract UpdateContext getUpdateContext(Cell cell, TableCellElement cellElement, GPropertyDraw property, GGridPropertyTable<T>.GridPropertyColumn column);

    public ExecuteEditContext getEditContext(Cell editCell, TableCellElement editCellParent) {
        final GPropertyDraw property = GPropertyTable.this.getProperty(editCell);
        GGridPropertyTable<T>.GridPropertyColumn gridColumn = getGridColumn(editCell.getColumnIndex());
        RenderContext renderContext = getRenderContext(editCell, editCellParent, property, gridColumn);
        UpdateContext updateContext = getUpdateContext(editCell, editCellParent, property, gridColumn);
        Element editElement = GPropertyTableBuilder.getRenderSizedElement(editCellParent, property, updateContext);
        return new ExecuteEditContext() {
            @Override
            public RenderContext getRenderContext() {
                return renderContext;
            }

            @Override
            public UpdateContext getUpdateContext() {
                return updateContext;
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
            public void setValue(Object value) {
                setValueAt(editCell, value);
            }

            @Override
            public void setLoading() {
                setLoadingAt(editCell);
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

            @Override
            public void startEditing() {
            }

            @Override
            public void stopEditing() {
            }
        };
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
