package lsfusion.gwt.shared.actions.navigator;

public class ThrowInNavigatorAction extends NavigatorRequestAction {
    public Throwable throwable;

    public ThrowInNavigatorAction() {}

    public ThrowInNavigatorAction(Throwable throwable) {
        this.throwable = throwable;
    }
}
