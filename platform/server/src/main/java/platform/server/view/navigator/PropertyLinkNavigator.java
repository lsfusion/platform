package platform.server.view.navigator;

public class PropertyLinkNavigator extends ValueLinkNavigator {

    public PropertyLinkNavigator(PropertyObjectNavigator iProperty) {
        property = iProperty;
    }

    public final PropertyObjectNavigator property;
}
