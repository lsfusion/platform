package lsfusion.client.navigator;

public class ClientCaptionElementNavigator extends ClientElementNavigator {

    public ClientCaptionElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    public void update(ClientNavigatorElement rootElement, Object value) {
        ClientNavigatorElement navigatorElement = rootElement.findElementByCanonicalName(canonicalName);
        navigatorElement.caption = (String) value;
    }
}