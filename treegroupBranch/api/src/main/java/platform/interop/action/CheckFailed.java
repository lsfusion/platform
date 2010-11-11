package platform.interop.action;

import java.io.IOException;

public class CheckFailed implements ClientApply {

    public String message;

    public CheckFailed(String message) {
        this.message = message;
    }
}
