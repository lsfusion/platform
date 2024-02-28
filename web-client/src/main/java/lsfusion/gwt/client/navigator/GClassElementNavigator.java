package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.view.MainFrame;

public class GClassElementNavigator extends GElementNavigator {

    public GClassElementNavigator() {
    }

    public GClassElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void updateElement(GNavigatorElement element, PValue value) {
        element.elementClass = PValue.getClassStringValue(value);

        if(MainFrame.mobile) { // not mobile elements are updated in NavigatorController.update()
            MainFrame.mobileNavigatorView.updateElementClass(element);
        }
    }
}
