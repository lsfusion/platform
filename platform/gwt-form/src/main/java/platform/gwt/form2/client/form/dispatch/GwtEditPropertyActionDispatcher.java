package platform.gwt.form2.client.form.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.cell.client.Cell;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.client.form.ui.GGridTable;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.GUserInputResult;
import platform.gwt.view2.GridDataRecord;
import platform.gwt.view2.actions.GRequestUserInputAction;
import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.classes.GType;

public class GwtEditPropertyActionDispatcher extends GwtFormActionDispatcher {

    private GGridTable editTable;

    private GType readType;
    private Object oldValue;
    private GPropertyDraw simpleChangeProperty;
    private GGroupObjectValue editColumnKey;

    private boolean valueRequested;


    public GwtEditPropertyActionDispatcher(GFormController form) {
        super(form);
    }

    public void executePropertyEditAction(final GGridTable table, Object oldValue, Cell.Context context) {
        editTable = table;
        final GridDataRecord record = (GridDataRecord) context.getKey();

        valueRequested = false;
        simpleChangeProperty = null;
        readType = null;
        editColumnKey = null;
        final GPropertyDraw property = table.getProperty(context.getColumn());

        if (property.changeType != null) {
            editColumnKey = record.key;
            simpleChangeProperty = property;
            startEdit(simpleChangeProperty.changeType, oldValue);
            return;
        }

        form.executeEditAction(property, record.key, "change", new ErrorAsyncCallback<ServerResponseResult>() {
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
            startEdit(editType, oldValue);
        }
    }

    public void startEdit(GType type, Object oldValue) {
        Log.debug("Edit started.");
        valueRequested = true;
        editTable.startEditing(type, oldValue);
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
