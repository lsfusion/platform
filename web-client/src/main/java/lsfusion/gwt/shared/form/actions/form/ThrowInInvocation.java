package lsfusion.gwt.shared.form.actions.form;

public class ThrowInInvocation extends FormAction<ServerResponseResult> {
    public Throwable throwable;

    public ThrowInInvocation() {
    }

    public ThrowInInvocation(Throwable throwable) {
        this.throwable = throwable;
    }
}
