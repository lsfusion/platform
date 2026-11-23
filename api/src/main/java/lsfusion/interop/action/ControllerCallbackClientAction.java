package lsfusion.interop.action;

import java.io.IOException;

/**
 * Base of the terminal actions delivering a form-controller exec/eval/change outcome back to the JS callback
 * correlated by the dispatching response's requestIndex - no id is carried on the action (GFORM-CONTROLLER-EXEC-EVAL-PLAN §12.3/§12.4):
 *  - {@link ControllerResultClientAction} — a result (its serialized {@code values}+{@code types}; a null value
 *    ⇒ no/undefined value) -> onResult;
 *  - {@link ControllerExceptionClientAction} — a business/property/cancel error -> onException.
 *
 * The web client converts them to GWT actions via {@code @Converter}. The desktop client calls a controller only for
 * itself, with no JS callback: it drops a result and raises an error.
 */
public abstract class ControllerCallbackClientAction implements ClientAction {


    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return null;
    }
}
