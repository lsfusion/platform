package paas.manager.common;

import java.io.Serializable;

public class NotificationData implements Serializable {
    public int configurationId;
    public String eventType;
    public String message;

    public NotificationData(int configurationId, String eventType, String message) {
        this.configurationId = configurationId;
        this.eventType = eventType;
        this.message = message;
    }

    @Override
    public String toString() {
        return "{" +
                "configurationId=" + configurationId +
                ", eventType='" + eventType + '\'' +
                (message != null ? ", message='" + message + '\'' : "") +
                "}";
    }
}
