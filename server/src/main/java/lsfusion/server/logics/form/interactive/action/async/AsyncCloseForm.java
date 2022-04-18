package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncCloseForm extends AsyncExec {
    public String canonicalName;

    public AsyncCloseForm(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    @Override
    public byte getTypeId() {
        return 2;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        SerializationUtil.writeString(outStream, canonicalName);
    }
}