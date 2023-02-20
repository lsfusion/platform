package lsfusion.gwt.client.navigator;

public class GCaptionElementNavigator extends GElementNavigator {

    public GCaptionElementNavigator() {
        super();
    }

    public GCaptionElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void updateElement(GNavigatorElement result, Object value) {
        result.caption = (String) value;
    }
}