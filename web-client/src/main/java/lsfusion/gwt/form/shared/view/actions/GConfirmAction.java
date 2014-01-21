package lsfusion.gwt.form.shared.view.actions;

public class GConfirmAction  implements GAction {
    public String message;
    public String caption;
    public boolean cancel;
    public int timeout;
    public int initialValue;
    
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GConfirmAction() {}

    public GConfirmAction(String message, String caption, boolean cancel, int timeout, int initialValue) {
        this.message = message;
        this.caption = caption;
        this.cancel = cancel;
        this.timeout = timeout;
        this.initialValue = initialValue;
    }

    public final Object dispatch(GActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}