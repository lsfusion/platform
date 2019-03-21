package lsfusion.interop.navigator;

public class PushMessage extends LifecycleMessage {
    public Integer idNotification;

    public PushMessage(Integer idNotification) {
        this.idNotification = idNotification;
    }
}