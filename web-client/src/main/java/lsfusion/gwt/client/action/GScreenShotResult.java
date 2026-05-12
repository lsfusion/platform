package lsfusion.gwt.client.action;

import java.io.Serializable;

public class GScreenShotResult implements Serializable {
    public byte[] data;

    @SuppressWarnings("UnusedDeclaration")
    public GScreenShotResult() {}

    public GScreenShotResult(byte[] data) {
        this.data = data;
    }
}
