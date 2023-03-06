package lsfusion.client.navigator.window;

import lsfusion.client.navigator.ClientPropertyNavigator;

public abstract class ClientWindowNavigator extends ClientPropertyNavigator {

    public final String canonicalName;

    public ClientWindowNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }
}
