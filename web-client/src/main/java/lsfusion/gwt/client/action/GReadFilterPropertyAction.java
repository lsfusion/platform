package lsfusion.gwt.client.action;

public class GReadFilterPropertyAction implements GAction {
    public Integer property;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GReadFilterPropertyAction() {
    }

    public GReadFilterPropertyAction(Integer property) {
        this.property = property;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}