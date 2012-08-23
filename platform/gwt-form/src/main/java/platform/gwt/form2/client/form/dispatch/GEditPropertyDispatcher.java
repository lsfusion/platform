package platform.gwt.form2.client.form.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GUserInputResult;
import platform.gwt.form2.shared.view.actions.GAsyncResultAction;
import platform.gwt.form2.shared.view.actions.GRequestUserInputAction;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.classes.GType;

public class GEditPropertyDispatcher extends GFormActionDispatcher {

    private GEditPropertyHandler editHandler;

    private GType readType;
    private Object oldValue;
    private GPropertyDraw simpleChangeProperty;
    private GGroupObjectValue editColumnKey;

    private boolean valueRequested;


    public GEditPropertyDispatcher(GFormController form) {
        super(form);
    }

    public void executePropertyEditAction(final GEditPropertyHandler ieditHandler, GPropertyDraw editProperty, Object oldValue, GGroupObjectValue columnKey) {
        editHandler = ieditHandler;

        valueRequested = false;
        simpleChangeProperty = null;
        readType = null;
        editColumnKey = null;

        if (editProperty.changeType != null) {
            editColumnKey = columnKey;
            simpleChangeProperty = editProperty;
            requestValue(simpleChangeProperty.changeType, oldValue);
            return;
        }

        form.executeEditAction(editProperty, columnKey, "change", new ErrorAsyncCallback<ServerResponseResult>() {
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
            requestValue(editType, oldValue);
        }
    }

    public void requestValue(GType type, Object oldValue) {
        Log.debug("Edit started.");
        valueRequested = true;
        editHandler.requestValue(type, oldValue);
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

    @Override
    public void execute(GAsyncResultAction action) {
        editHandler.updateEditValue(action.value);
    }
}
