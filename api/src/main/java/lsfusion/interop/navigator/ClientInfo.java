package lsfusion.interop.navigator;

import lsfusion.interop.connection.ClientType;

import java.io.Serializable;

public class ClientInfo implements Serializable {

    public String screenSize;
    public Double scale;
    public ClientType clientType;
    public boolean initial;

    public ClientInfo() {
    }

    public ClientInfo(String screenSize, Double scale, ClientType clientType, boolean initial) {
        this.screenSize = screenSize;
        this.scale = scale;
        this.clientType = clientType;
        this.initial = initial;
    }
}
