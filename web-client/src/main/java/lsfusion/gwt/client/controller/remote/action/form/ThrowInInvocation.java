package lsfusion.gwt.client.controller.remote.action.form;

public class ThrowInInvocation extends FormRequestAction<ServerResponseResult> {
    public Throwable throwable; // assert that throwable is already converted
    public int continueIndex;

    public ThrowInInvocation() {
    }

    public ThrowInInvocation(long requestIndex, Throwable throwable, int continueIndex) {
        super(requestIndex);
        this.throwable = throwable;
        this.continueIndex = continueIndex;
    }
}
