package lsfusion.gwt.client.action;

import java.io.Serializable;
import java.util.List;

public class GFilterAction extends GExecuteAction {
    public int goID;
    public List<FilterItem> filters;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFilterAction() {
    }

    public GFilterAction(int goID, List<FilterItem> filters) {
        this.goID = goID;
        this.filters = filters;
    }
    
    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }

    public static class FilterItem implements Serializable {
        public int propertyId = -1;
        public int compare = -1;
        public boolean negation = false;
        public Serializable value;
        public boolean junction;

        @SuppressWarnings("UnusedDeclaration")
        public FilterItem() {
        }

        public FilterItem(int propertyId, int compare, boolean negation, Serializable value, boolean junction) {
            this.propertyId = propertyId;
            this.compare = compare;
            this.negation = negation;
            this.value = value;
            this.junction = junction;
        }
    }
}
