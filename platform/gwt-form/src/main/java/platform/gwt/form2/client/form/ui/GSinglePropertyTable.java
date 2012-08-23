package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.cellview.client.DataGrid;
import platform.gwt.form2.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form2.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GridDataRecord;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.changes.dto.ColorDTO;
import platform.gwt.form2.shared.view.classes.GType;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.GridEditableCell;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;

import java.util.Arrays;

public class GSinglePropertyTable extends DataGrid implements EditManager, GEditPropertyHandler {
    /**
     * Default style's overrides
     */
    public interface GSinglePropertyTableResource extends Resources {
        @Source("GSinglePropertyTable.css")
        GSinglePropertyTableStyle dataGridStyle();
    }
    public interface GSinglePropertyTableStyle extends Style {}

    public static final GSinglePropertyTableResource GSINGLE_PROPERTY_TABLE_RESOURCE = GWT.create(GSinglePropertyTableResource.class);

    private final GFormController form;
    private final GEditPropertyDispatcher editDispatcher;
    private final GPropertyDraw property;
    private Object value;
    private GridDataRecord valueRecord;

    private GridEditableCell editCell;
    private Cell.Context editContext;
    private Element editCellParent;
    private GType editType;

    public GSinglePropertyTable(GFormController iform, GPropertyDraw iproperty) {
        super(50, GSINGLE_PROPERTY_TABLE_RESOURCE);

        this.form = iform;
        this.property = iproperty;

        this.editDispatcher = new GEditPropertyDispatcher(form);

        addColumn(property.createGridColumn(this, form));
        setRowData(Arrays.asList(new Object()));
    }


    public void setValue(Object value) {
        this.value = value;
        this.valueRecord = new GridDataRecord(property.sID, value);
        setRowData(Arrays.asList(valueRecord));
    }

    public void setBackgroundColor(ColorDTO color) {
        //todo:
    }

    public void setForegroundColor(ColorDTO color) {
        //todo:
    }

    public GPropertyDraw getProperty(int column) {
        return property;
    }

    @Override
    public void requestValue(GType valueType, Object oldValue) {
        editType = valueType;

        GridCellEditor cellEditor = valueType.createGridCellEditor(this, getProperty(editContext.getColumn()), oldValue);
        if (cellEditor != null) {
            editCell.startEditing(editContext, editCellParent, cellEditor);
        } else {
            cancelEditing();
        }
    }

    @Override
    public void updateEditValue(Object value) {
        //todo:
    }

    @Override
    public boolean isCurrentlyEditing() {
        //todo: возвращать true, если любая таблица редактируется, чтобы избежать двойного редактирования...
        return editType != null;
    }

    @Override
    public void executePropertyEditAction(GridEditableCell editCell, Cell.Context editContext, Element parent) {
        this.editCell = editCell;
        this.editContext = editContext;
        this.editCellParent = parent;
        GGroupObjectValue columnKey = ((GridDataRecord) editContext.getKey()).key;
        editDispatcher.executePropertyEditAction(this, property, value, columnKey);
    }

    @Override
    public void commitEditing(Object value) {
        clearEditState();
        editDispatcher.commitValue(value);
    }

    @Override
    public void cancelEditing() {
        clearEditState();
        editDispatcher.cancelEdit();
    }

    private void clearEditState() {
        editCell.finishEditing(editContext, editCellParent, value);

        editCell = null;
        editContext = null;
        editCellParent = null;
        editType = null;
    }

}
