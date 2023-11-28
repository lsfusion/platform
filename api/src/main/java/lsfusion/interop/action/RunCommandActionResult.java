package lsfusion.interop.action;

import java.io.Serializable;

public class RunCommandActionResult implements Serializable {
    private final String cmdOut;
    private final String cmdErr;
    private final int exitValue;

    public RunCommandActionResult(String cmdOut, String cmdErr, int exitValue) {
        this.cmdOut = cmdOut;
        this.cmdErr = cmdErr;
        this.exitValue = exitValue;
    }

    public String getCmdOut() {
        return cmdOut;
    }

    public String getCmdErr() {
        return cmdErr;
    }

    public boolean isCompletedSuccessfully() {
        return exitValue == 0;
    }

    public String getErrorMessage() {
        return "exitValue = " + exitValue + "\n" + cmdErr;
    }
}
