package lsfusion.gwt.client.action;

public class GReadFilterGroupAction implements GAction {
    public Integer filterGroup;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GReadFilterGroupAction() {
    }

    public GReadFilterGroupAction(Integer filterGroup) {
        this.filterGroup = filterGroup;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}