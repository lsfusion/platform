package lsfusion.gwt.form.shared.actions.navigator;

public class ThrowInNavigatorAction extends NavigatorRequestAction {
    public Throwable throwable;

    public ThrowInNavigatorAction() {}

    public ThrowInNavigatorAction(String tabSID, Throwable throwable) {
        super(tabSID);
        this.throwable = throwable;
    }
}
