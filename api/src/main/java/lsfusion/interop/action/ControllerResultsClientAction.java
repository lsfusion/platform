package lsfusion.interop.action;

/**
 * The RESULTS of a form-controller exec/eval whose result has parameters (a RETURN of a property with a parameter
 * left free): every value with its name - the parameters it is of -, delivered to the JS callback's onResult as an
 * array of {name, value} (see {@link ControllerCallbackClientAction}). A result without parameters is one value, a
 * {@link ControllerResultClientAction}.
 */
public class ControllerResultsClientAction extends ControllerCallbackClientAction {

    public final String[] names;
    public final ControllerResultClientAction[] results;

    public ControllerResultsClientAction(String[] names, ControllerResultClientAction[] results) {
        this.names = names;
        this.results = results;
    }
}
