package lsfusion.interop.action;

public class RequestUserInputClientAction implements ClientAction {
    public final byte[] readType;
    public final byte[] oldValue;
    public final boolean hasOldValue;
    
    public final byte[] inputList;

    public RequestUserInputClientAction(byte[] readType, byte[] oldValue, boolean hasOldValue, byte[] inputList) {
        this.readType = readType;
        this.oldValue = oldValue;
        this.hasOldValue = hasOldValue;
        
        this.inputList = inputList;
//        assert hasOldValue || oldValue == null;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}
