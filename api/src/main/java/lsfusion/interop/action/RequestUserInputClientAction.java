package lsfusion.interop.action;

public class RequestUserInputClientAction implements ClientAction {
    public final byte[] readType;
    public final byte[] oldValue;
    public final boolean hasOldValue;

    public final String customChangeFunction;
    
    public final byte[] inputList;
    public final byte[] inputListActions;

    public RequestUserInputClientAction(byte[] readType, byte[] oldValue, boolean hasOldValue, String customChangeFunction,
                                        byte[] inputList, byte[] inputListActions) {
        this.readType = readType;
        this.oldValue = oldValue;
        this.hasOldValue = hasOldValue;

        this.customChangeFunction = customChangeFunction;

        this.inputList = inputList;
        this.inputListActions = inputListActions;
//        assert hasOldValue || oldValue == null;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}
