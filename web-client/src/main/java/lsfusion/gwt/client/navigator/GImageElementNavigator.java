package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.base.AppStaticImage;

public class GImageElementNavigator extends GElementNavigator {

    public GImageElementNavigator() {
        super();
    }

    public GImageElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void update(GNavigatorElement root, Object value) {
        GNavigatorElement result = findNavigatorElementByCanonicalName(root);
        if(result != null) {
            if(value instanceof AppStaticImage) { //static image
                result.image = (AppStaticImage) value;
            } else if(value instanceof String) { //dynamic image
                result.dynamicImage = (String) value;
            }
        }
    }
}