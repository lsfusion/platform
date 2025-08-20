package lsfusion.gwt.client.action.net;

import java.io.Serializable;

public class GTcpResult implements Serializable {
    public byte[] response;

    public GTcpResult() {
    }

    public GTcpResult(byte[] response) {
        this.response = response;
    }
}