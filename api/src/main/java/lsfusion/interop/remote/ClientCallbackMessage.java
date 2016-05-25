package lsfusion.interop.remote;

public class ClientCallbackMessage extends LifecycleMessage {
    public CallbackMessage message;

    public ClientCallbackMessage(CallbackMessage message) {
        this.message = message;
    }
}