package lsfusion.interop.action;

/**
 * A form-controller exec/eval/change RESULT delivered to the JS callback's onResult (see
 * {@link ControllerCallbackClientAction}). {@code value == null} ⇒ no/undefined value; otherwise the serialized
 * value + type are converted to a JS value on the web client.
 */
public class ControllerResultClientAction extends ControllerCallbackClientAction {

    public final byte[] value;
    public final byte[] type;

    public ControllerResultClientAction(byte[] value, byte[] type) {
        this.value = value;
        this.type = type;
    }
}
