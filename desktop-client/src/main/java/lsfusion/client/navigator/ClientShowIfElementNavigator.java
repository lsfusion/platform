package lsfusion.client.navigator;

public class ClientShowIfElementNavigator extends ClientElementNavigator {

    public ClientShowIfElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    public void update(ClientNavigatorElement rootElement, Object value) {
        ClientNavigatorElement navigatorElement = rootElement.findElementByCanonicalName(canonicalName);
        navigatorElement.hide = value == null;
    }
}