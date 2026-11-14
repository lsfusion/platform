package lsfusion.gwt.client.action;

// the RESULTS of a form-controller exec/eval whose result has parameters -> resolves the JS callback with an array of
// {name, value}, a value being what a GControllerResultAction resolves with
public class GControllerResultsAction extends GControllerCallbackAction {
    public String[] names;
    public GControllerResultAction[] results;

    @SuppressWarnings("UnusedDeclaration")
    public GControllerResultsAction() {}

    public GControllerResultsAction(String[] names, GControllerResultAction[] results) {
        this.names = names;
        this.results = results;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
