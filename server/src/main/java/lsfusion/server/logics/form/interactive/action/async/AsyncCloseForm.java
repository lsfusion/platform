package lsfusion.server.logics.form.interactive.action.async;

import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncCloseForm extends AsyncExec {
    public AsyncCloseForm() {
    }

    @Override
    public byte getTypeId() {
        return 2;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
    }
}