package lsfusion.client.form.property.async;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;

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

    public ClientAsyncOpenForm(String canonicalName, String caption, boolean forbidDuplicate, boolean modal) {
        this.canonicalName = canonicalName;
        this.caption = caption;
        this.forbidDuplicate = forbidDuplicate;
        this.modal = modal;
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        this.canonicalName = pool.readString(inStream);
        this.caption = pool.readString(inStream);
        this.forbidDuplicate = pool.readBoolean(inStream);
        this.modal = pool.readBoolean(inStream);
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) {
        if(!isModal()) { //ignore async modal windows in desktop
            ((DockableMainFrame) MainFrame.instance).asyncOpenForm(form.getRmiQueue().getNextRmiRequestIndex(), this);
        }
        return true;
    }

    private boolean isModal() {
        //if current form is modal, new async form can't be non-modal
        ClientFormController currentForm = MainFrame.instance.currentForm;
        return modal || (currentForm != null && currentForm.isModal());
    }

    @Override
    public void exec() {
        ((DockableMainFrame) (MainFrame.instance)).asyncOpenForm(this);
    }
}
