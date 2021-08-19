package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.BaseUtils;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncOpenForm extends AsyncExec {
    public String canonicalName;
    public String caption;
    public boolean forbidDuplicate;
    public boolean modal;
    public boolean window;

    public AsyncOpenForm(String canonicalName, String caption, boolean forbidDuplicate, boolean modal, boolean window) {
        this.canonicalName = canonicalName;
        this.caption = caption;
        this.forbidDuplicate = forbidDuplicate;
        this.modal = modal;
        this.window = window;
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
        outStream.writeBoolean(forbidDuplicate);
        outStream.writeBoolean(modal);
        outStream.writeBoolean(window);
    }
}