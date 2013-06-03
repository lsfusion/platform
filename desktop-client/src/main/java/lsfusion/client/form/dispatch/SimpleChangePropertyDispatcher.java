package lsfusion.client.form.dispatch;

import com.google.common.base.Throwables;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.form.UserInputResult;

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

    public boolean changeProperty(Object value, ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        this.value = value;

        try {
            dispatchResponse(getFormController().executeEditAction(property, columnKey, ServerResponse.CHANGE));
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
