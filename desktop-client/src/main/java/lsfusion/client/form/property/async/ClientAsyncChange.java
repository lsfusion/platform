package lsfusion.client.form.property.async;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

public class ClientAsyncChange extends ClientAsyncFormExec implements Serializable {

    public int[] propertyIDs;

    public Serializable value;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncChange() {
    }

    public ClientAsyncChange(DataInputStream inStream) throws IOException {
        super(inStream);

        int size = inStream.readInt();
        propertyIDs = new int[size];
        for(int i=0;i<size;i++)
            propertyIDs[i] = inStream.readInt();
        this.value = (Serializable) BaseUtils.deserializeObject(inStream);
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        return false;
    }

    @Override
    public boolean isDesktopEnabled(boolean canShowDockedModal) {
        return false;
    }
}
