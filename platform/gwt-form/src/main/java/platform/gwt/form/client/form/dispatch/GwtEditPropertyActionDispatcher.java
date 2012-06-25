package platform.gwt.form.client.form.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickEvent;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.client.form.ui.GGridTable;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.sgwtbase.client.ErrorAsyncCallback;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.GUserInputResult;
import platform.gwt.view.GridDataRecord;
import platform.gwt.view.actions.GRequestUserInputAction;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.classes.GType;

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

    public void executePropertyEditAction(final GGridTable table, CellDoubleClickEvent event) {
        editTable = table;
        editRow = event.getRowNum();
        editCol = event.getColNum();
        final GridDataRecord record = (GridDataRecord) event.getRecord();
        final GPropertyDraw property = table.getProperty(editCol);

        valueRequested = false;
        simpleChangeProperty = null;
        readType = null;
        editColumnKey = null;

        if (property.changeType != null) {
            editColumnKey = record.key;
            simpleChangeProperty = property;
            startEdit(table, simpleChangeProperty.changeType, table.getValueAt(editRow, editCol));
            return;
        }


        form.disable();
        form.executeEditAction(property, record.key, "change", new ErrorAsyncCallback<ServerResponseResult>() {
            @Override
            public void preProcess() {
                Log.debug("Execute edit action response recieved...");
                form.enable();
            }

            @Override
            public void success(ServerResponseResult response) {
                dispatchResponse(response);

                if (readType != null) {
                    startEdit(table, readType, oldValue);
                }
            }
        });

    }

    @Override
    public void dispatchResponse(ServerResponseResult response) {
        super.dispatchResponse(response);

        if (readType != null) {
            startEdit(editTable, readType, oldValue);
            readType = null;
        }
    }

    public void startEdit(GGridTable table, GType type, Object oldValue) {
        valueRequested = true;
        table.editCellAt(type, oldValue, editRow, editCol);
    }


    public void cancelEdit() {
        internalCommitValue(GUserInputResult.canceled);
    }

    public void commitValue(Object newValue) {
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
        oldValue = action.oldValue.getValue();

        pauseDispatching();

        return null;
    }
}
