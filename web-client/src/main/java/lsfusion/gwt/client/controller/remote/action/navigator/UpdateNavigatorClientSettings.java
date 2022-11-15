package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.base.result.VoidResult;

public class UpdateNavigatorClientSettings extends NavigatorPriorityAction<VoidResult>{
    public String screenSize;
    public boolean mobile;

    public UpdateNavigatorClientSettings(String screenSize, boolean mobile) {
        this.screenSize = screenSize;
        this.mobile = mobile;
    }

    public UpdateNavigatorClientSettings() {
    }
}
