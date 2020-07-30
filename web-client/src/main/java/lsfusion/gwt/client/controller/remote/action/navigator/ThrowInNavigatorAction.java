package lsfusion.gwt.client.controller.remote.action.navigator;

public class ThrowInNavigatorAction extends NavigatorRequestAction {
    public Throwable throwable; // assert that throwable is already converted
    public int continueIndex;

    public ThrowInNavigatorAction() {}

    public ThrowInNavigatorAction(Throwable throwable, long requestIndex, int continueIndex) {
        super(requestIndex);
        this.throwable = throwable;
        this.continueIndex = continueIndex;
    }
}
