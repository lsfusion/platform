package lsfusion.gwt.form.shared.view.actions;

public class GActivateTabAction extends GExecuteAction {
    public String formSID;
    public String tabSID;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GActivateTabAction() {}

    public GActivateTabAction(String formSID, String tabSID) {
        this.formSID = formSID;
        this.tabSID = tabSID;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}