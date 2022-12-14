package lsfusion.client.navigator;

import java.io.Serializable;

public abstract class ClientPropertyNavigator implements Serializable {

    public abstract void update(ClientNavigatorElement rootElement, Object value);

}