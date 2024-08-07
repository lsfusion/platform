package lsfusion.gwt.client.action;

public class GFilterGroupAction extends GExecuteAction {
    public Integer filterGroup;
    public Integer index;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFilterGroupAction() {
    }

    public GFilterGroupAction(Integer filterGroup, Integer index) {
        this.filterGroup = filterGroup;
        this.index = index;
    }
    
    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}