package lsfusion.server.logics.form.struct.property.async;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class AsyncExec extends AsyncEventExec {

    public abstract int getType();

    public abstract void serialize(DataOutputStream outStream) throws IOException;

}