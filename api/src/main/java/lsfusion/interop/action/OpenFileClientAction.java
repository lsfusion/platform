package lsfusion.interop.action;

import java.io.IOException;

public class OpenFileClientAction extends ExecuteClientAction {

    public final byte[] file;
    public final String name;
    public final String extension;

    public OpenFileClientAction(byte[] file, String name, String extension) {
        this.file = file;
        this.name = name;
        this.extension = extension;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
