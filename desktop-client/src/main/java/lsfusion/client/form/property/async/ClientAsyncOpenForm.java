package lsfusion.client.form.property.async;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientAsyncOpenForm extends ClientAsyncExec {
    public String canonicalName;
    public String caption;
    public boolean forbidDuplicate;
    public boolean modal;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncOpenForm() {
    }

    public ClientAsyncOpenForm(DataInputStream inStream) throws IOException {
        super(inStream);

        this.canonicalName = SerializationUtil.readString(inStream);
        this.caption = SerializationUtil.readString(inStream);
        this.forbidDuplicate = inStream.readBoolean();
        this.modal = inStream.readBoolean();
    }

    public ClientAsyncOpenForm(String canonicalName, String caption, boolean forbidDuplicate, boolean modal) {
        this.canonicalName = canonicalName;
        this.caption = caption;
        this.forbidDuplicate = forbidDuplicate;
        this.modal = modal;
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        form.asyncOpenForm(property, columnKey, actionSID, this);
        return true;
    }

    public boolean isModal() {
        //if current form is modal, new async form can't be non-modal
        ClientFormController currentForm = MainFrame.instance.currentForm;
        return modal || (currentForm != null && currentForm.isModal());
    }

    @Override
    public void exec() {
        ((DockableMainFrame) (MainFrame.instance)).asyncOpenForm(this);
    }
}
