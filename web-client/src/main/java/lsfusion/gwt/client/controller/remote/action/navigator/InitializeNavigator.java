package lsfusion.gwt.client.controller.remote.action.navigator;

public class InitializeNavigator extends NavigatorPriorityAction<InitializeNavigatorResult>{
    public String timeZone;
    public String screenSize;
    public Double scale;
    public boolean mobile;

    public InitializeNavigator() {
    }

    public InitializeNavigator(String timeZone, String screenSize, Double scale, boolean mobile) {
        this.timeZone = timeZone;
        this.screenSize = screenSize;
        this.scale = scale;
        this.mobile = mobile;
    }

}