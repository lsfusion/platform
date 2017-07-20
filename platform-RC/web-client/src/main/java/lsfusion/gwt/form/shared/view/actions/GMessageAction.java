package lsfusion.gwt.form.shared.view.actions;

public class GMessageAction extends GExecuteAction {
    public String message;
    public String caption;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GMessageAction() {}

    public GMessageAction(String message, String caption) {
        this(false, message, caption);
    }

    public GMessageAction(boolean failed, String message, String caption) {
        this.message = message;
        this.caption = caption;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
