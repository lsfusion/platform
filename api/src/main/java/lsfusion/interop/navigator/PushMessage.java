package lsfusion.interop.navigator;

public class PushMessage extends LifecycleMessage {
    public Integer idNotification;
    /** Initial delay before the client fires the notification (milliseconds, 0 for immediate). */
    public long delay;
    /** When non-null, the client re-fires the notification every period ms (fire-and-forget). */
    public Long period;

    public PushMessage(Integer idNotification, long delay, Long period) {
        this.idNotification = idNotification;
        this.delay = delay;
        this.period = period;
    }
}