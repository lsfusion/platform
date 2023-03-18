package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.view.MainFrame;

public class GCaptionElementNavigator extends GElementNavigator {

    public GCaptionElementNavigator() {
        super();
    }

    public GCaptionElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void updateElement(GNavigatorElement result, Object value) {
        result.caption = value != null ? value.toString() : null;

        if(MainFrame.mobile) { // not mobile elements are updated in NavigatorController.update()
            MainFrame.mobileNavigatorView.updateText(result);
        }
    }
}