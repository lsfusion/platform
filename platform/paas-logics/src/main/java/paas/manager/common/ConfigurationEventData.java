package paas.manager.common;

import java.io.Serializable;

public class ConfigurationEventData implements Serializable {
    public int configurationId;
    public Object data;

    public ConfigurationEventData(int configurationId, Object data) {
        this.configurationId = configurationId;
        this.data = data;
    }

    @Override
    public String toString() {
        return "{configurationId=" + configurationId + ", data=" + data + '}';
    }
}
