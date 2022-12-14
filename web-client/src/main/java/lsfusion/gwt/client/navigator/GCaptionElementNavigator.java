package lsfusion.gwt.client.navigator;

public class GCaptionElementNavigator extends GElementNavigator {

    public GCaptionElementNavigator() {
        super();
    }

    public GCaptionElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void update(GNavigatorElement root, Object value) {
        GNavigatorElement result = findNavigatorElementByCanonicalName(root);
        if(result != null) {
            result.caption = (String) value;
        }
    }

    private GNavigatorElement findNavigatorElementByCanonicalName(GNavigatorElement root) {
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