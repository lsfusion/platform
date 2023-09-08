package lsfusion.gwt.client.action;

import java.util.LinkedHashMap;

public class GOrderAction extends GExecuteAction {
    public int goID;
    public LinkedHashMap<Integer, Boolean> ordersMap;
    
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GOrderAction() {
    }

    public GOrderAction(int goID, LinkedHashMap<Integer, Boolean> ordersMap) {
        this.goID = goID;
        this.ordersMap = ordersMap;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}