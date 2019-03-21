package lsfusion.gwt.client.controller.remote.action.form;

public class ThrowInInvocation extends FormAction<ServerResponseResult> {
    public Throwable throwable;

    public ThrowInInvocation() {
    }

    public ThrowInInvocation(Throwable throwable) {
        this.throwable = throwable;
    }
}
