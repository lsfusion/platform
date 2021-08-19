package lsfusion.client.form.property.async;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class ClientAsyncEventExec {

    public abstract boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException;

    public ClientAsyncEventExec() {
    }

    public ClientAsyncEventExec(DataInputStream inStream) {
    }

    public boolean isDesktopEnabled(boolean canShowDockedModal) {
        return true;
    }
}
