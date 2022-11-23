package lsfusion.gwt.client.controller.remote.action.navigator;

public class InitializeNavigator extends NavigatorPriorityAction<InitializeNavigatorResult>{
    public String screenSize;
    public boolean mobile;

    public InitializeNavigator() {
    }

    public InitializeNavigator(String screenSize, boolean mobile) {
        this.screenSize = screenSize;
        this.mobile = mobile;
    }

}