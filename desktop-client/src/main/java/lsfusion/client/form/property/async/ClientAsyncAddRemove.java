package lsfusion.client.form.property.async;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientAsyncAddRemove extends ClientAsyncInputExec {
    // we could use ClientObject, but there is no context when deserializing ClientForm, so clientform will be null (it could be fixed by setting context in customDeserialize, but it's simpler to make everything symmetric with GAsyncAddRemove)
    public int object;
    public Boolean add;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncAddRemove() {
    }

    public ClientAsyncAddRemove(DataInputStream inStream) throws IOException {
        super(inStream);

        this.object = inStream.readInt();
        this.add = inStream.readBoolean();
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        form.asyncAddRemove(property, columnKey, actionSID, this);
        return true;
    }
}
