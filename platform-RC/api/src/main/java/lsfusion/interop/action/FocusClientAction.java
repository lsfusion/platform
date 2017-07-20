package lsfusion.interop.action;

import java.io.IOException;

public class FocusClientAction extends ExecuteClientAction {

    public int propertyId;

    public FocusClientAction(int propertyId) {
        this.propertyId = propertyId;
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "FocusClientAction[propertyId: " + propertyId + "]";
    }
}
