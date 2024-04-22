package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.controller.FormsController;
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
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.ArrayList;
import java.util.List;

public abstract class GPropertyTable<T extends GridDataRecord> extends DataGrid<T> {

    protected final GFormController form;
    protected final GGroupObject groupObject;

    public GPropertyTable(GFormController iform, GGroupObject groupObject, TableContainer tableContainer, boolean noHeaders, boolean noFooters) {
        super(tableContainer, noHeaders, noFooters);

        this.form = iform;
        this.groupObject = groupObject;

        //  Have the enter key work the same as the tab key
        form.addEnterBindings(GBindingMode.ONLY, ((GGridPropertyTable) GPropertyTable.this)::selectNextCellInColumn, this.groupObject);
    }

    public abstract Boolean isReadOnly(Cell cell);

    public abstract GPropertyDraw getSelectedProperty();
    public abstract GGroupObjectValue getSelectedColumnKey();

    public abstract GPropertyDraw getProperty(Cell cell);
    public abstract GGridPropertyTable<T>.GridPropertyColumn getGridColumn(int column);

    @Override
    public boolean isChangeOnSingleClick(Cell cell, Event event, boolean rowChanged, Column column) {
        GPropertyDraw property = getProperty(cell);
        if(property != null && property.changeOnSingleClick != null)
            return property.changeOnSingleClick;
        return super.isChangeOnSingleClick(cell, event, rowChanged, column) ||
                (!rowChanged && FormsController.isLinkMode() && property != null && property.hasEditObjectAction) ||
                (GMouseStroke.isChangeEvent(event) && GEditBindingMap.getToolbarAction(event) != null);
    }

    public abstract GGroupObjectValue getColumnKey(Cell editCell);

    public abstract GGroupObjectValue getRowKey(Cell editCell);

    public abstract void setValueAt(Cell cell, PValue value);

    public abstract void setLoadingAt(Cell cell);

    public abstract void pasteData(Cell cell, Element renderElement, List<List<String>> table);

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

    protected abstract RenderContext getRenderContext(Cell cell, Element renderElement, GPropertyDraw property, GGridPropertyTable<T>.GridPropertyColumn column);

    protected abstract UpdateContext getUpdateContext(Cell cell, Element renderElement, GPropertyDraw property, GGridPropertyTable<T>.GridPropertyColumn column);

    public ExecuteEditContext getEditContext(Cell editCell, Element editRenderElement) {
        final GPropertyDraw property = GPropertyTable.this.getProperty(editCell);
        GGridPropertyTable<T>.GridPropertyColumn gridColumn = getGridColumn(editCell.getColumnIndex());
        RenderContext renderContext = getRenderContext(editCell, editRenderElement, property, gridColumn);
        UpdateContext updateContext = getUpdateContext(editCell, editRenderElement, property, gridColumn);
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
                return editRenderElement;
            }

            @Override
            public void setValue(PValue value) {
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
            public Boolean isReadOnly() {
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
            public void focus(FocusUtils.Reason reason) {
                GPropertyTable.this.focusColumn(editCell.getColumnIndex(), reason);
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

            @Override
            public RendererType getRendererType() {
                return RendererType.GRID;
            }
        };
    }

    public abstract GFont getFont();

    @Override
    protected String getGridInfo() {
        return form.form.sID + " " + groupObject.sID;
    }
}
