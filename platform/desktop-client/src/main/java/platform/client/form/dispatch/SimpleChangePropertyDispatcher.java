package platform.client.form.dispatch;

import com.google.common.base.Throwables;
import platform.client.form.ClientFormController;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.action.RequestUserInputClientAction;
import platform.interop.form.ServerResponse;
import platform.interop.form.UserInputResult;

import java.io.IOException;

public class SimpleChangePropertyDispatcher extends ClientFormActionDispatcher {
    private final ClientFormController form;
    private Object value = null;

    public SimpleChangePropertyDispatcher(ClientFormController form) {
        this.form = form;
    }

    @Override
    public ClientFormController getFormController() {
        return form;
    }

    public boolean changeProperty(Object value, ClientPropertyDraw property, ClientGroupObjectValue columnKey, boolean isChangeWYS) {
        this.value = value;

        try {
            dispatchResponse(getFormController().executeEditAction(property, columnKey, isChangeWYS ? ServerResponse.CHANGE_WYS : ServerResponse.CHANGE));
            return true;
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }

    @Override
    public Object execute(RequestUserInputClientAction action) {
        return new UserInputResult(value);
    }
}
