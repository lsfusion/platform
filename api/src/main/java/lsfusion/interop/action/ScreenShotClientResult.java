package lsfusion.interop.action;

import java.io.Serializable;

public class ScreenShotClientResult implements Serializable {
    public byte[] data;

    public ScreenShotClientResult() {}

    public ScreenShotClientResult(byte[] data) {
        this.data = data;
    }
}
