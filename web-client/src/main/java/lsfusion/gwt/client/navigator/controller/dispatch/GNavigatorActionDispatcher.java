package lsfusion.gwt.client.navigator.controller.dispatch;

import lsfusion.gwt.client.action.GActivateFormAction;
import lsfusion.gwt.client.action.GCloseFormAction;
import lsfusion.gwt.client.action.GFormAction;
import lsfusion.gwt.client.action.GMaximizeFormAction;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ContinueNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.navigator.ThrowInNavigatorAction;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

public class GNavigatorActionDispatcher extends GwtActionDispatcher {
    
    private final WindowsController windowsController;
    private final FormsController formsController;

    public GNavigatorActionDispatcher(WindowsController windowsController, FormsController formsController) {
        this.windowsController = windowsController;
        this.formsController = formsController;
    }

    @Override
    protected void throwInServerInvocation(long requestIndex, Throwable t, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        MainFrame.syncDispatch(new ThrowInNavigatorAction(t, requestIndex, continueIndex), callback, true);
    }

    @Override
    protected void continueServerInvocation(long requestIndex, Object[] actionResults, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        MainFrame.syncDispatch(new ContinueNavigatorAction(actionResults, requestIndex, continueIndex), callback, true);
    }

    @Override
    public void execute(final GFormAction action) {
        if (action.showFormType.isModal()) {
            pauseDispatching();
        }
        formsController.openForm(getAsyncFormController(getDispatchingIndex()), action.form, action.showFormType, action.forbidDuplicate, null, null, null, () -> {
            if(action.showFormType.isDocked() || action.showFormType.isDockedModal())
                formsController.ensureTabSelected();

            if (action.showFormType.isModal()) {
                continueDispatching();
            }
        }, action.formId);
    }

    @Override
    protected void onServerInvocationResponse(ServerResponseResult response) {
        formsController.onServerInvocationResponse(response, getAsyncFormController(response.requestIndex));
    }

    @Override
    public void execute(final GActivateFormAction action) {
        formsController.selectTab(action.formCanonicalName);
    }

    @Override
    public void execute(final GMaximizeFormAction action) {
        if (!MainFrame.mobile) {
            formsController.setFullScreenMode(true);
        }
    }

    @Override
    public void execute(final GCloseFormAction action) {
        formsController.closeForm(action.formId);
    }
}
