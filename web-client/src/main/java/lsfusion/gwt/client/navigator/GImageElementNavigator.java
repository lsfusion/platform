package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.base.AppStaticImage;

public class GImageElementNavigator extends GElementNavigator {

    public GImageElementNavigator() {
        super();
    }

    public GImageElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void updateElement(GNavigatorElement result, Object value) {
        result.image = (AppBaseImage) value; // was converted in convertFileValue
    }
}