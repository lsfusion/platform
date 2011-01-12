package platform.interop.action;

import java.io.IOException;

public class OpenFileClientAction extends AbstractClientAction {

    public final byte[] file;
    public final String extension;

    public OpenFileClientAction(byte[] file, String extension) {
        this.file = file;
        this.extension = extension;
    }

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
