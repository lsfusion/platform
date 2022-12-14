package lsfusion.gwt.client.navigator;

public abstract class GElementNavigator extends GPropertyNavigator {
    public String canonicalName;

    public GElementNavigator() {
    }

    public GElementNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public abstract void update(GNavigatorElement root, Object value);
}