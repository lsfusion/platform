package lsfusion.gwt.form.shared.view.actions;

public class GOpenUriAction extends GExecuteAction {
    public String uri;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GOpenUriAction() {}

    public GOpenUriAction(String uri) {
        this.uri = uri;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
