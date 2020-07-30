package lsfusion.gwt.client.navigator;

import java.io.Serializable;

public class ConnectionInfo implements Serializable {
    public String screenSize;
    public boolean mobile;

    public ConnectionInfo() {
    }

    public ConnectionInfo(String screenSize, boolean mobile) {
        this.screenSize = screenSize;
        this.mobile = mobile;
    }
}