package lsfusion.client.form.property.async;

import java.io.DataInputStream;
import java.util.function.Function;

public abstract class ClientAsyncExec extends ClientAsyncEventExec {

    public long exec(Function<ClientPushAsyncResult, Long> execute, boolean ctrl, boolean sync) {
        return execute.apply(null);
    }

    public ClientAsyncExec() {
    }

    public ClientAsyncExec(DataInputStream inStream) {
        super(inStream);
    }
}
