package lsfusion.client.form.property.async;

import lsfusion.client.classes.ClientType;
import lsfusion.client.classes.ClientTypeSerializer;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientAsyncChange extends ClientAsyncInputExec {
    public ClientType changeType;

    public ClientInputList inputList;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncChange() {
    }

    public ClientAsyncChange(DataInputStream inStream) throws IOException {
        super(inStream);

        this.changeType = ClientTypeSerializer.deserializeClientType(inStream);
        if(inStream.readBoolean())
            this.inputList = ClientAsyncSerializer.deserializeInputList(inStream);

        if (inStream.readBoolean())
            customEditorFunction = inStream.readUTF();
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        return dispatcher.asyncChange(property, columnKey, actionSID, this);
    }
}
