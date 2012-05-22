package platform.interop.action;

import java.io.IOException;

public class ChooseClassAction implements ClientAction {

    public final byte[] classes;
    public final boolean concrete;

    public ChooseClassAction(byte[] classes, boolean concrete) {
        this.classes = classes;
        this.concrete = concrete;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}
