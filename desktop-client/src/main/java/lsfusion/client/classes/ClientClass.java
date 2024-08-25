package lsfusion.client.classes;

import java.io.Serializable;

abstract public class ClientClass implements Serializable {
    public abstract boolean hasChildren();

    protected ClientClass() {
    }
}
