package lsfusion.client.navigator.window;

import lsfusion.client.navigator.ClientNavigatorElement;

// the class ClientNavigatorChangesToGwtConverter dispatches on to produce a GCustomWindowNavigator; only the desktop
// client's own update is a no-op, since it draws its regular toolbar
public class ClientCustomWindowNavigator extends ClientWindowNavigator {

    public ClientCustomWindowNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void update(ClientNavigatorElement rootElement, Object value) {
    }
}
