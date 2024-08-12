package lsfusion.gwt.client.action;

public class GFilterPropertyAction extends GExecuteAction {
    public Integer property;
    public String value;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFilterPropertyAction() {
    }

    public GFilterPropertyAction(Integer property, String value) {
        this.property = property;
        this.value = value;
    }
    
    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}