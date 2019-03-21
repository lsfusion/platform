package lsfusion.gwt.client.controller.remote.action.navigator;

public class ThrowInNavigatorAction extends NavigatorRequestAction {
    public Throwable throwable;

    public ThrowInNavigatorAction() {}

    public ThrowInNavigatorAction(Throwable throwable) {
        this.throwable = throwable;
    }
}
