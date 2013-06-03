package lsfusion.interop.action;

import java.io.IOException;

public class RequestUserInputClientAction implements ClientAction {
    public final byte[] readType;
    public final byte[] oldValue;

    public RequestUserInputClientAction(byte[] readType, byte[] oldValue) {
        this.readType = readType;
        this.oldValue = oldValue;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}
