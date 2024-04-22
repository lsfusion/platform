package lsfusion.interop.navigator;

import lsfusion.interop.connection.ClientType;

import java.io.Serializable;

public class ClientInfo implements Serializable {

    public String screenSize;
    public ClientType clientType;

    public ClientInfo() {
    }

    public ClientInfo(String screenSize, ClientType clientType) {
        this.screenSize = screenSize;
        this.clientType = clientType;
    }
}
