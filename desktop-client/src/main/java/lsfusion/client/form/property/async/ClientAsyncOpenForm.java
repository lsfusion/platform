package lsfusion.client.form.property.async;

import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.ModalityWindowFormType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientAsyncOpenForm extends ClientAsyncExec {
    public String canonicalName;
    public String caption;
    public AppImage appImage;
    public boolean forbidDuplicate;
    public boolean modal;
    public WindowFormType type;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncOpenForm() {
    }

    public ClientAsyncOpenForm(DataInputStream inStream) throws IOException {
        super(inStream);

        this.canonicalName = SerializationUtil.readString(inStream);
        this.caption = SerializationUtil.readString(inStream);
        appImage = IOUtils.readAppImage(inStream);
        this.forbidDuplicate = inStream.readBoolean();
        this.modal = inStream.readBoolean();
        this.type = WindowFormType.deserialize(inStream);
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        form.asyncOpenForm(property, dispatcher, columnKey, actionSID, this);
        return true;
    }

    @Override
    public void exec(long requestIndex) {
        ((DockableMainFrame) (MainFrame.instance)).asyncOpenForm(this, requestIndex);
    }

    public boolean isDesktopEnabled(boolean canShowDockedModal) { // should correspond SwingClientActionDispatcher.getModalityType
        return type == ModalityWindowFormType.DOCKED && !(modal && !canShowDockedModal);
    }
}
