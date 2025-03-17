package lsfusion.gwt.client.controller.remote.action.navigator;

public class InitializeNavigator extends NavigatorPriorityAction<InitializeNavigatorResult>{
    public Integer width;
    public Integer height;
    public Double scale;

    public InitializeNavigator() {
    }

    public InitializeNavigator(Integer width, Integer height, Double scale) {
        this.width = width;
        this.height = height;
        this.scale = scale;
    }

}