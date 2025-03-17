package lsfusion.interop.navigator;

import lsfusion.interop.connection.ClientType;

import java.io.Serializable;

public class ClientInfo implements Serializable {
    public Integer screenWidth;
    public Integer screenHeight;
    public Double scale;
    public ClientType clientType;
    public boolean initial;

    public ClientInfo() {
    }

    public ClientInfo(Integer screenWidth, Integer screenHeight, Double scale, ClientType clientType, boolean initial) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.scale = scale;
        this.clientType = clientType;
        this.initial = initial;
    }
}
