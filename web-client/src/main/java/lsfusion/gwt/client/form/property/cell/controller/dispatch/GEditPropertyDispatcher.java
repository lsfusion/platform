package lsfusion.gwt.client.form.property.cell.controller.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import lsfusion.gwt.client.action.GRequestUserInputAction;
import lsfusion.gwt.client.action.GUpdateEditValueAction;
import lsfusion.gwt.client.base.exception.ErrorHandlingCallback;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.controller.dispatch.GFormActionDispatcher;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.controller.GEditPropertyHandler;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

public class GEditPropertyDispatcher extends GFormActionDispatcher {

    private final GEditPropertyHandler editHandler;

    private GType readType;
    private Object oldValue;
    private boolean hasOldValue;
    private GPropertyDraw simpleChangeProperty;
    private GGroupObjectValue editColumnKey;

    private boolean valueRequested;
    private Object oldValueRequested;
    private boolean transferFocusAfterEdit;


    public GEditPropertyDispatcher(GFormController form, GEditPropertyHandler editHandler) {
        super(form);
        this.editHandler = editHandler;
    }

    public void executePropertyEventAction(final GPropertyDraw editProperty, final GGroupObjectValue columnKey, String actionSID) {
        valueRequested = false;
        oldValueRequested = null;
        oldValue = null;
        hasOldValue = false;

        simpleChangeProperty = null;
        readType = null;
        editColumnKey = null;

        transferFocusAfterEdit = true;

        final boolean asyncModifyObject = form.isAsyncModifyObject(editProperty);
        if (GEditBindingMap.CHANGE.equals(actionSID) && (asyncModifyObject || editProperty.changeType != null)) {
            if (editProperty.askConfirm) {
                form.blockingConfirm("lsFusion", editProperty.askConfirmMessage, false, new DialogBoxHelper.CloseCallback() {
                    @Override
                    public void closed(DialogBoxHelper.OptionType chosenOption) {
                        if (chosenOption == DialogBoxHelper.OptionType.YES) {
                            executeSimpleChangeProperty(asyncModifyObject, editProperty, columnKey);
                        } else {
                            transferFocusAfterEdit();
                        }
                    }
                });
            } else {
                executeSimpleChangeProperty(asyncModifyObject, editProperty, columnKey);
            }
        } else {
            form.executeEventAction(editProperty, columnKey, actionSID, new ErrorHandlingCallback<ServerResponseResult>() {
                @Override
                public void success(ServerResponseResult response) {
                    Log.debug("Execute edit action response received...");
                    dispatchResponse(response);
                }
            });
        }
    }

    private void transferFocusAfterEdit() {
        if (transferFocusAfterEdit) {
            editHandler.takeFocusAfterEdit();
        }
    }

    private void executeSimpleChangeProperty(boolean asyncModifyObject, GPropertyDraw editProperty, GGroupObjectValue columnKey) {
        if (asyncModifyObject) {
            form.modifyObject(editProperty, columnKey);
            transferFocusAfterEdit();
        } else {
//          т.е. property.changeType != null
            editColumnKey = columnKey;
            simpleChangeProperty = editProperty;
            requestValue(simpleChangeProperty.changeType);
        }
    }

    @Override
    public void dispatchResponse(ServerResponseResult response, int continueIndex) {
        super.dispatchResponse(response, continueIndex);
        if (readType != null) {
            GType editType = readType;
            readType = null;
//            GExceptionManager.addStackTrace("DROPPED READTYPE");
            requestValue(editType);
        }
    }

    @Override
    protected void postDispatchResponse(ServerResponseResult response) {
        super.postDispatchResponse(response);
        transferFocusAfterEdit();
    }

    private void requestValue(GType type) {
        Log.debug("Edit started.");
        valueRequested = true;
        oldValueRequested = editHandler.getEditValue();
        editHandler.requestValue(type, hasOldValue ? oldValue : oldValueRequested);
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

        valueRequested = false;
        Object oldValueRequested = this.oldValueRequested;
        this.oldValueRequested = null;

        if (simpleChangeProperty != null) {
            if (!inputResult.isCanceled()) {
                if (simpleChangeProperty.canUseChangeValueForRendering()) {
                    editHandler.updateEditValue(inputResult.getValue());
                }
                form.changeProperty(simpleChangeProperty, editColumnKey, inputResult.getValue(), oldValueRequested);
                transferFocusAfterEdit();
            }
            return;
        }

        continueDispatching(inputResult);
    }

    @Override
    public Object execute(GRequestUserInputAction action) {
        readType = action.readType;
        oldValue = action.oldValue;
//        GExceptionManager.addStackTrace("SET READTYPE");

        pauseDispatching();

        return null;
    }

    @Override
    public void execute(GUpdateEditValueAction action) {
        editHandler.updateEditValue(action.value);
    }
}
