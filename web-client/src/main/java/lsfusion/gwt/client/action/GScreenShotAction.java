package lsfusion.gwt.client.action;

public class GScreenShotAction implements GAction {
    public boolean html;
    public String containerSID;

    @SuppressWarnings("UnusedDeclaration")
    public GScreenShotAction() {}

    public GScreenShotAction(boolean html, String containerSID) {
        this.html = html;
        this.containerSID = containerSID;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
