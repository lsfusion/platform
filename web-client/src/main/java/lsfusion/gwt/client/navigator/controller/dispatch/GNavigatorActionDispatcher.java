package lsfusion.gwt.client.navigator.controller.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.action.GActivateFormAction;
import lsfusion.gwt.client.action.GFormAction;
import lsfusion.gwt.client.action.GMaximizeFormAction;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ContinueNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.navigator.ThrowInNavigatorAction;
import lsfusion.gwt.client.form.controller.DefaultFormsController;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

public class GNavigatorActionDispatcher extends GwtActionDispatcher {
    
    private final WindowsController windowsController;
    private final DefaultFormsController formsController;

    public GNavigatorActionDispatcher(WindowsController windowsController, DefaultFormsController formsController) {
        this.windowsController = windowsController;
        this.formsController = formsController;
    }

    @Override
    protected void throwInServerInvocation(long requestIndex, Throwable t, int continueIndex, AsyncCallback<ServerResponseResult> callback) {
        MainFrame.navigatorDispatchAsync.execute(new ThrowInNavigatorAction(t, requestIndex, continueIndex), callback);
    }

    @Override
    protected void continueServerInvocation(long requestIndex, Object[] actionResults, int continueIndex, AsyncCallback<ServerResponseResult> callback) {
        MainFrame.navigatorDispatchAsync.execute(new ContinueNavigatorAction(actionResults, requestIndex, continueIndex), callback);
    }

    @Override
    public void execute(final GFormAction action) {
        if (action.modalityType.isModal()) {
            pauseDispatching();
        }
        formsController.openForm(action.form, action.modalityType, action.forbidDuplicate, null, () -> {
            if (action.modalityType.isModal()) {
                continueDispatching();
            }
        });
    }

    @Override
    public void execute(final GActivateFormAction action) {
        formsController.selectTab(action.formCanonicalName);
    }

    @Override
    public void execute(final GMaximizeFormAction action) {
        windowsController.setFullScreenMode(true);
    }
}
