package lsfusion.client.navigator;

import java.io.Serializable;

public abstract class ClientPropertyNavigator implements Serializable {

    public final String canonicalName;

    public ClientPropertyNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public abstract void update(ClientNavigatorElement rootElement, Object value);
}