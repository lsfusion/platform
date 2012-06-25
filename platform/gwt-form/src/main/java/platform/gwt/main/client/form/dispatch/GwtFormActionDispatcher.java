package platform.gwt.main.client.form.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import platform.gwt.main.client.form.ui.GFormController;
import platform.gwt.main.shared.actions.form.ServerResponseResult;
import platform.gwt.view.actions.GConfirmAction;
import platform.gwt.view.actions.GMessageAction;
import platform.gwt.view.actions.GProcessFormChangesAction;

public class GwtFormActionDispatcher extends GwtActionDispatcher {
    protected final GFormController form;

    public GwtFormActionDispatcher(GFormController form) {
        this.form = form;
    }

    @Override
    public void execute(GMessageAction action) {
        //todo: sync this....
        SC.say(action.caption, action.message);
    }

    @Override
    public void execute(GProcessFormChangesAction action) {
        form.applyRemoteChanges(action.formChanges);
    }

    @Override
    protected void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        form.contiueServerInvocation(actionResults, callback);
    }

    @Override
    protected void throwInServerInvocation(Exception ex) {
        form.throwInServerInvocation(ex);
    }

    @Override
    public int execute(GConfirmAction action) {
        pauseDispatching();
        form.disable();
        SC.confirm(action.caption, action.message, new BooleanCallback() {
            @Override
            public void execute(Boolean value) {
                form.enable();
                continueDispatching(value ? YES_OPTION : NO_OPTION);
            }
        });

        return 0;
    }
}
