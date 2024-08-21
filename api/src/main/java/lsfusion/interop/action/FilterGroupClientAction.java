package lsfusion.interop.action;

public class FilterGroupClientAction extends ExecuteClientAction {

    public Integer filterGroup;
    public Integer index;

    public FilterGroupClientAction(Integer filterGroup, Integer index) {
        this.filterGroup = filterGroup;
        this.index = index;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}