package lsfusion.interop.action;

import java.io.IOException;

/**
 * Base of the terminal actions delivering a form-controller exec/eval/change outcome back to the JS callback
 * registered under {@code callbackId} (GFORM-CONTROLLER-EXEC-EVAL-PLAN §12.3/§12.4):
 *  - {@link ControllerResultClientAction} — a result (serialized {@code value}+{@code type}; {@code value == null}
 *    ⇒ no/undefined value) -> onResult;
 *  - {@link ControllerExceptionClientAction} — a business/property/cancel error -> onException.
 *
 * Web-only: produced by RemoteForm for browser-originated controller calls and converted to a GWT action via
 * {@code @Converter}; it never reaches a desktop client, so {@link #dispatch} is unsupported.
 */
public abstract class ControllerCallbackClientAction implements ClientAction {

    public final long callbackId;

    protected ControllerCallbackClientAction(long callbackId) {
        this.callbackId = callbackId;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        throw new UnsupportedOperationException("controller callback actions are web-only");
    }
}
