package lsfusion.interop.action;

import java.io.IOException;

public class RequestUserInputClientAction implements ClientAction {
    public final byte[] readType;
    public final byte[] oldValue;
    public final boolean hasOldValue;
    
    public RequestUserInputClientAction(byte[] readType, byte[] oldValue, boolean hasOldValue) {
        this.readType = readType;
        this.oldValue = oldValue;
        this.hasOldValue = hasOldValue;
//        assert hasOldValue || oldValue == null;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}
