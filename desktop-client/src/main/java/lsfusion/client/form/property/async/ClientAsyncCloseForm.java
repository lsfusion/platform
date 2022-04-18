package lsfusion.client.form.property.async;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientAsyncCloseForm extends ClientAsyncExec {
    public String canonicalName;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncCloseForm() {
    }

    public ClientAsyncCloseForm(DataInputStream inStream) throws IOException {
        super(inStream);

        this.canonicalName = SerializationUtil.readString(inStream);
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        //do nothing
        return true;
    }

    @Override
    public void exec(long requestIndex) {
        //do nothing
    }

    @Override
    public boolean isDesktopEnabled(boolean canShowDockedModal) {
        return false;
    }
}