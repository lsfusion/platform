package platform.gwt.form2.client.form.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.client.form.ui.GGridTable;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.GUserInputResult;
import platform.gwt.view2.actions.GRequestUserInputAction;
import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.classes.GType;

public class GwtEditPropertyActionDispatcher extends GwtFormActionDispatcher {
    private GType readType;
    private Object oldValue;
    private GPropertyDraw simpleChangeProperty;
    private GGroupObjectValue editColumnKey;

    private boolean valueRequested;

    private GGridTable editTable;
    private int editRow;
    private int editCol;


    public GwtEditPropertyActionDispatcher(GFormController form) {
        super(form);
    }

//    public void executePropertyEditAction(final GGridTable table, CellDoubleClickEvent event) {
    public void executePropertyEditAction(final GGridTable table) {
        editTable = table;
//        editRow = event.getRowNum();
//        editCol = event.getColNum();
//        final GridDataRecord record = (GridDataRecord) event.getRecord();
        final GPropertyDraw property = table.getProperty(editCol);

        valueRequested = false;
        simpleChangeProperty = null;
        readType = null;
        editColumnKey = null;

        if (property.changeType != null) {
//            editColumnKey = record.key;
            simpleChangeProperty = property;
            startEdit(table, simpleChangeProperty.changeType, table.getValueAt(editRow, editCol));
            return;
        }

//        form.executeEditAction(property, record.key, "change", new ErrorAsyncCallback<ServerResponseResult>() {
        form.executeEditAction(property, null, "change", new ErrorAsyncCallback<ServerResponseResult>() {
            @Override
            public void success(ServerResponseResult response) {
                Log.debug("Execute edit action response recieved...");
                dispatchResponse(response);
            }
        });
    }

    @Override
    public void dispatchResponse(ServerResponseResult response) {
        super.dispatchResponse(response);

        if (readType != null) {
            GType editType = readType;
            readType = null;
            startEdit(editTable, editType, oldValue);
        }
    }

    public void startEdit(GGridTable table, GType type, Object oldValue) {
        Log.debug("Edit started.");
        valueRequested = true;
        table.editCellAt(type, oldValue, editRow, editCol);
    }

    public void cancelEdit() {
        Log.debug("Edit canceled.");
        internalCommitValue(GUserInputResult.canceled);
    }

    public void commitValue(Object newValue) {
        Log.debug("Edit commit:" + newValue);
        internalCommitValue(new GUserInputResult(newValue));
    }

    private void internalCommitValue(GUserInputResult inputResult) {
        if (!valueRequested) {
            throw new IllegalStateException("value wasn't requested");
        }

        if (simpleChangeProperty != null) {
            if (!inputResult.isCanceled()) {
                form.changeProperty(simpleChangeProperty, inputResult.getValue());
            }
            return;
        }

        continueDispatching(inputResult);
    }

    @Override
    public Object execute(GRequestUserInputAction action) {
        readType = action.readType;
        oldValue = action.oldValue;

        pauseDispatching();

        return null;
    }
}
