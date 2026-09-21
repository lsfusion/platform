package lsfusion.client.form.property.async;

import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.FormActivateType;
import lsfusion.interop.form.DockedWindowFormType;
import lsfusion.interop.form.ModalityWindowFormType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.function.Function;

public class ClientAsyncOpenForm extends ClientAsyncExec {
    public String canonicalName;
    public String caption;
    public AppImage appImage;
    public FormActivateType activateType;
    public String formId;
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
        this.activateType = FormActivateType.deserialize(inStream.readByte());
        this.formId = SerializationUtil.readString(inStream);
        this.modal = inStream.readBoolean();
        this.type = WindowFormType.deserialize(inStream);
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        form.asyncOpenForm(property, dispatcher, columnKey, actionSID, this);
        return true;
    }

    @Override
    public long exec(Function<ClientPushAsyncResult, Long> execute, boolean ctrl, boolean sync) {
        DockableMainFrame mainFrame = (DockableMainFrame) MainFrame.instance;
        ClientPushAsyncResult push = mainFrame.reuseOpenForm(this, ctrl);
        long requestIndex = execute.apply(push);
        if (!sync && push == null)
            mainFrame.asyncOpenForm(this, requestIndex);
        return requestIndex;
    }

    public boolean isDesktopEnabled(boolean canShowDockedModal) { // should correspond SwingClientActionDispatcher.getModalityType
        return type instanceof DockedWindowFormType && !(modal && !canShowDockedModal); // a named window opens as a tab here, see SwingClientActionDispatcher.getShowFormType
    }
}
