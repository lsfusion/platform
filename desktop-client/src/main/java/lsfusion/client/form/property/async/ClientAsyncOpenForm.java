package lsfusion.client.form.property.async;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.FormsController;
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
    public String caption;
    public boolean modal;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncOpenForm() {
    }

    public ClientAsyncOpenForm(String caption, boolean modal) {
        this.caption = caption;
        this.modal = modal;
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        this.caption = pool.readString(inStream);
        this.modal = pool.readBoolean(inStream);
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        if(!modal) { //ignore async modal windows in desktop
            ((DockableMainFrame) MainFrame.instance).asyncOpenForm(form.getRmiQueue().getNextRmiRequestIndex(), this);
        }
        return true;
    }

    @Override
    public void exec() {
        ((DockableMainFrame) (MainFrame.instance)).asyncOpenForm(this);
    }
}
