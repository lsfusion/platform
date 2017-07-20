package lsfusion.gwt.form.shared.actions.form;

public class ThrowInInvocation extends FormBoundAction<ServerResponseResult> {
    public Throwable throwable;

    public ThrowInInvocation() {
    }

    public ThrowInInvocation(Throwable throwable) {
        this.throwable = throwable;
    }
}
