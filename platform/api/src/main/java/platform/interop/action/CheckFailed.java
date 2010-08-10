package platform.interop.action;

public class CheckFailed extends ClientApply {

    public String message;

    public CheckFailed(String message) {
        this.message = message;
    }
}
