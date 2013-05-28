package platform.gwt.form.shared.view.actions;

public class GConfirmAction  implements GAction {
    public String message;
    public String caption;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GConfirmAction() {}

    public GConfirmAction(String message, String caption) {
        this.message = message;
        this.caption = caption;
    }

    public final Object dispatch(GActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}