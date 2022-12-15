package lsfusion.gwt.client.navigator;

public abstract class GElementNavigator extends GPropertyNavigator {
    public String canonicalName;

    public GElementNavigator() {
    }

    public GElementNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public abstract void update(GNavigatorElement root, Object value);

    protected GNavigatorElement findNavigatorElementByCanonicalName(GNavigatorElement root) {
        for(GNavigatorElement child : root.children) {
            if(child.canonicalName.equals(canonicalName)) {
                return child;
            } else {
                GNavigatorElement element = findNavigatorElementByCanonicalName(child);
                if(element != null) {
                    return element;
                }
            }
        }
        return null;
    }
}