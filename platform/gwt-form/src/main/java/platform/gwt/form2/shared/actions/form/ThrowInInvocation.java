package platform.gwt.form2.shared.actions.form;

public class ThrowInInvocation extends FormBoundAction<ServerResponseResult> {
    public Exception exception;

    public ThrowInInvocation() {
    }

    public ThrowInInvocation(Exception exception) {
        this.exception = exception;
    }
}
