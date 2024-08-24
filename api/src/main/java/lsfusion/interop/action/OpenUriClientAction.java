package lsfusion.interop.action;

import java.io.IOException;


public class OpenUriClientAction extends ExecuteClientAction {

    public byte[] uri;
    public boolean noEncode;

    public OpenUriClientAction(byte[] uri, boolean noEncode) {
        this.uri = uri;
        this.noEncode = noEncode;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
