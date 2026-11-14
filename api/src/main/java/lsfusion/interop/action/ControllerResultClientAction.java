package lsfusion.interop.action;

/**
 * A form-controller exec/eval/change RESULT delivered to the JS callback's onResult (see
 * {@link ControllerCallbackClientAction}): its values, each serialized with its type and converted to a JS value on the
 * web client ({@code values[i] == null} ⇒ no/undefined value). A result without parameters is one value, which the
 * callback gets; a result with them (a RETURN of a property with a parameter left free) is every value with its name -
 * the parameters it is of -, which the callback gets as an array of {name, value}.
 */
public class ControllerResultClientAction extends ControllerCallbackClientAction {

    public final String[] names; // of a result with parameters; null - the result has none
    public final byte[][] values;
    public final byte[][] types;

    public ControllerResultClientAction(String[] names, byte[][] values, byte[][] types) {
        this.names = names;
        this.values = values;
        this.types = types;
    }
}
