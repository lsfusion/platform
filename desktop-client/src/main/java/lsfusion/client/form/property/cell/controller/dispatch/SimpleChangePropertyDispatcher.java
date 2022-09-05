package lsfusion.client.form.property.cell.controller.dispatch;

import com.google.common.base.Throwables;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.dispatch.ClientFormActionDispatcher;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.property.cell.UserInputResult;

import java.io.IOException;

public class SimpleChangePropertyDispatcher extends ClientFormActionDispatcher {
    private final ClientFormController form;
    private Object value = null;

    public SimpleChangePropertyDispatcher(ClientFormController form) {
        super(form.getDispatcherListener());
        this.form = form;
    }

    @Override
    public ClientFormController getFormController() {
        return form;
    }

    public boolean changeProperty(Object value, ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        this.value = value;

        try {
            ServerResponse serverResponse = getFormController().executeEventAction(property, columnKey, ServerResponse.CHANGE, null);
            try {
                dispatchServerResponse(serverResponse);
            } finally {
                if (dispatcherListener != null && serverResponse != ServerResponse.EMPTY)  // проверка нужна, если запрос заблокируется то и postponeDispatchingEnded не будет, а значит "скобки" нарушатся и упадет assertion
                    dispatcherListener.dispatchingPostponedEnded(this);
            }
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
