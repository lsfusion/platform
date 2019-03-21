package lsfusion.gwt.client.navigator.controller.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.MainFrame;
import lsfusion.gwt.client.form.controller.DefaultFormsController;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.client.navigator.window.WindowsController;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.actions.navigator.ContinueNavigatorAction;
import lsfusion.gwt.shared.actions.navigator.ThrowInNavigatorAction;
import lsfusion.gwt.shared.view.actions.GActivateFormAction;
import lsfusion.gwt.shared.view.actions.GFormAction;
import lsfusion.gwt.shared.view.actions.GMaximizeFormAction;

public class GNavigatorActionDispatcher extends GwtActionDispatcher {
    
    private final WindowsController windowsController;
    private final DefaultFormsController formsController;

    public GNavigatorActionDispatcher(WindowsController windowsController, DefaultFormsController formsController) {
        this.windowsController = windowsController;
        this.formsController = formsController;
    }

    @Override
    protected void throwInServerInvocation(Throwable t, AsyncCallback<ServerResponseResult> callback) {
        MainFrame.navigatorDispatchAsync.execute(new ThrowInNavigatorAction(t), callback);
    }

    @Override
    protected void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        MainFrame.navigatorDispatchAsync.execute(new ContinueNavigatorAction(actionResults), callback);
    }

    @Override
    public void execute(final GFormAction action) {
        if (action.modalityType.isModal()) {
            pauseDispatching();
        }
        formsController.openForm(action.form, action.modalityType, action.forbidDuplicate, null, new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                if (action.modalityType.isModal()) {
                    continueDispatching();
                }
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
