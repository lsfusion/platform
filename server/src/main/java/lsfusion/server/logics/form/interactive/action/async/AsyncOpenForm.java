package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.server.base.AppServerImage;
import lsfusion.base.file.IOUtils;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncOpenForm extends AsyncExec {
    public String canonicalName;
    public String caption;
    public AppServerImage image;
    public boolean forbidDuplicate;
    public boolean modal;
    public WindowFormType type;

    public AsyncOpenForm(String canonicalName, String caption, AppServerImage image, boolean forbidDuplicate, boolean modal, WindowFormType type) {
        this.canonicalName = canonicalName;
        this.caption = caption;
        this.image = image;
        this.forbidDuplicate = forbidDuplicate;
        this.modal = modal;
        this.type = type;
    }

    @Override
    public byte getTypeId() {
        return 1;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        SerializationUtil.writeString(outStream, canonicalName);
        SerializationUtil.writeString(outStream, caption);
        AppServerImage.serialize(image, outStream);
        outStream.writeBoolean(forbidDuplicate);
        outStream.writeBoolean(modal);
        type.serialize(outStream);
    }
}