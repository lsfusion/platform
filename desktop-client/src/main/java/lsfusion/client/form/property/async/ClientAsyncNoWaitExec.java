package lsfusion.client.form.property.async;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;

import java.io.IOException;

public class ClientAsyncNoWaitExec extends ClientAsyncExec{
    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        return false;
    }

    @Override
    public void exec(long requestIndex) {
    }

    @Override
    public boolean isDesktopEnabled(boolean canShowDockedModal) {
        return false;
    }
}