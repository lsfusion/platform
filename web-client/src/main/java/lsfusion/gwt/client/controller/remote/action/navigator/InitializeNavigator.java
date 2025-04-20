package lsfusion.gwt.client.controller.remote.action.navigator;

public class InitializeNavigator extends NavigatorPriorityAction<InitializeNavigatorResult>{
    public Integer width;
    public Integer height;
    public Double scale;

    public boolean prefetching;

    public InitializeNavigator() {
    }

    public InitializeNavigator(Integer width, Integer height, Double scale, boolean prefetching) {
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.prefetching = prefetching;
    }

}