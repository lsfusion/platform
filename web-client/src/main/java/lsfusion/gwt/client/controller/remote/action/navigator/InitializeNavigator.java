package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.view.GColorTheme;

public class InitializeNavigator extends NavigatorPriorityAction<InitializeNavigatorResult>{
    public Integer width;
    public Integer height;
    public Double scale;
    public GColorTheme colorTheme; // the color scheme the browser asks for

    public boolean prefetching;

    public InitializeNavigator() {
    }

    public InitializeNavigator(Integer width, Integer height, Double scale, GColorTheme colorTheme, boolean prefetching) {
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.colorTheme = colorTheme;
        this.prefetching = prefetching;
    }

}