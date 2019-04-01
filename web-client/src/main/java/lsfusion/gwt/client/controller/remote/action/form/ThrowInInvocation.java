package lsfusion.gwt.client.controller.remote.action.form;

public class ThrowInInvocation extends FormRequestIndexAction<ServerResponseResult> {
    public Throwable throwable;
    public int continueIndex;

    public ThrowInInvocation() {
    }

    public ThrowInInvocation(long requestIndex, Throwable throwable, int continueIndex) {
        super(requestIndex);
        this.throwable = throwable;
        this.continueIndex = continueIndex;
    }
}
