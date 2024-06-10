package lsfusion.interop.action;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class FilterClientAction extends ExecuteClientAction {
    public final int goID;
    public final List<FilterItem> filters;

    public FilterClientAction(int goID, List<FilterItem> filters) {
        this.goID = goID;
        this.filters = filters;
    }
    
    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
    
    public static class FilterItem implements Serializable {
        public int propertyId;
        public byte compare = -1;
        public boolean negation;
        public byte[] value;
        public boolean junction;
        
        public FilterItem(int propertyId) {
            this.propertyId = propertyId;
        }
        
    }
}
