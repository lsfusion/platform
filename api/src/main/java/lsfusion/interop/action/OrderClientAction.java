package lsfusion.interop.action;

import java.io.IOException;
import java.util.LinkedHashMap;

public class OrderClientAction extends ExecuteClientAction {
    public final int goID;
    public LinkedHashMap<Integer, Boolean> ordersMap;
    
    public OrderClientAction(int goID, LinkedHashMap<Integer, Boolean> ordersMap) {
        this.goID = goID;
        this.ordersMap = ordersMap;
    }
    
    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
