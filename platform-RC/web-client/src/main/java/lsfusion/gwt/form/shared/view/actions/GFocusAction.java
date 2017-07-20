package lsfusion.gwt.form.shared.view.actions;

public class GFocusAction extends GExecuteAction {
    public int propertyId;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFocusAction() {}

    public GFocusAction(int propertyId) {
        this.propertyId = propertyId;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
