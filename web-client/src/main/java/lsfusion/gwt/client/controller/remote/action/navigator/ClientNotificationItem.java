package lsfusion.gwt.client.controller.remote.action.navigator;

import java.io.Serializable;

/** Single notification delivery item: id plus optional client-side scheduling parameters. */
public class ClientNotificationItem implements Serializable {
    public Integer idNotification;
    /** Initial delay in milliseconds (0 = fire immediately). */
    public long delay;
    /** When non-null, the client re-fires the notification every period ms. */
    public Long period;

    public ClientNotificationItem() {
    }

    public ClientNotificationItem(Integer idNotification, long delay, Long period) {
        this.idNotification = idNotification;
        this.delay = delay;
        this.period = period;
    }
}
