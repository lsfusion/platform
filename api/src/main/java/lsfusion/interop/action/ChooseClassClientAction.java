package lsfusion.interop.action;

import java.io.IOException;

public class ChooseClassClientAction implements ClientAction {

    public final byte[] classes;
    public final boolean concrete;

    public ChooseClassClientAction(byte[] classes, boolean concrete) {
        this.classes = classes;
        this.concrete = concrete;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}
