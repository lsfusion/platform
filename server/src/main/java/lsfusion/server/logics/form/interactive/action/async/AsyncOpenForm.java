package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.server.base.AppServerImage;
import lsfusion.interop.form.DockedWindowFormType;
import lsfusion.interop.form.FormActivateType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncOpenForm extends AsyncExec {
    public String canonicalName;
    public String caption;
    public AppServerImage image;
    public FormActivateType activateType;
    public String formId; // the label the open would give the form, which is part of what says it is the same one
    public boolean modal;
    public WindowFormType type;

    public AsyncOpenForm(String canonicalName, String caption, AppServerImage image, FormActivateType activateType, String formId, boolean modal, WindowFormType type) {
        this.canonicalName = canonicalName;
        this.caption = caption;
        this.image = image;
        this.activateType = activateType;
        this.formId = formId;
        this.modal = modal;
        this.type = type;
    }

    @Override
    public PushAsyncResult deserializePush(DataInputStream inStream) throws IOException {
        // every open shares this type id, and only a docked one has a form a client can reuse: a confirmation of any
        // other is ignored, and the form is built as usual, rather than trusted or failed on
        if (!(type instanceof DockedWindowFormType))
            return null;

        // The client confirms this prediction; use its logical window even if the mobile layout remaps it.
        return new PushAsyncActivate(canonicalName, formId, ((DockedWindowFormType) type).window, activateType);
    }

    @Override
    public byte getTypeId() {
        return 1;
    }

    @Override
    public void serialize(ConnectionContext context, DataOutputStream outStream) throws IOException {
        super.serialize(context, outStream);

        SerializationUtil.writeString(outStream, canonicalName);
        SerializationUtil.writeString(outStream, caption);
        AppServerImage.serialize(image, outStream);
        outStream.writeByte(activateType != null ? activateType.serialize() : 0);
        SerializationUtil.writeString(outStream, formId);
        outStream.writeBoolean(modal);
        type.serialize(outStream);
    }
}
