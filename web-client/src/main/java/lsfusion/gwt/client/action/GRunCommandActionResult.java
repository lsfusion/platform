package lsfusion.gwt.client.action;

import java.io.Serializable;

public class GRunCommandActionResult implements Serializable {
    public String cmdOut;
    public String cmdErr;
    public int exitValue;

    public GRunCommandActionResult() {
    }

    public GRunCommandActionResult(String cmdOut, String cmdErr, int exitValue) {
        this.cmdOut = cmdOut;
        this.cmdErr = cmdErr;
        this.exitValue = exitValue;
    }
}