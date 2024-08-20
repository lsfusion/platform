package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.form.property.PValue;

public class GShowIfElementNavigator extends GElementNavigator {

    public GShowIfElementNavigator() {
        super();
    }

    public GShowIfElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void updateElement(GNavigatorElement result, PValue value) {
        result.hide = value == null;
    }
}