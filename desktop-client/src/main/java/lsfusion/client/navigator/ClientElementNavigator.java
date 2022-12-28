package lsfusion.client.navigator;

public abstract class ClientElementNavigator extends ClientPropertyNavigator {

    public final String canonicalName;

    public ClientElementNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }
}