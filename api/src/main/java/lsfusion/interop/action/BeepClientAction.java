package lsfusion.interop.action;

import java.io.IOException;

public class BeepClientAction extends ExecuteClientAction {

    public byte[] file;
    public boolean async;

    public BeepClientAction(byte[] file, boolean async) {
        this.file = file;
        this.async = async;
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}