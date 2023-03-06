package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.view.MainFrame;

public class GClassElementNavigator extends GElementNavigator {

    public GClassElementNavigator() {
    }

    public GClassElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void updateElement(GNavigatorElement element, Object value) {
        element.elementClass = (String) value;

        if(MainFrame.mobile) { // not mobile elements are updated in NavigatorController.update()
            MainFrame.mobileNavigatorView.updateElementClass(element);
        }
    }
}
