package lsfusion.interop.action;

public class FilterPropertyClientAction extends ExecuteClientAction {

    public Integer filterGroup;
    public String value;

    public FilterPropertyClientAction(Integer filterGroup, String value) {
        this.filterGroup = filterGroup;
        this.value = value;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}