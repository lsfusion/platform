package platform.gwt.paas.client.data;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import paas.api.gwt.shared.dto.ConfigurationDTO;

public class ConfigurationRecord extends BasicRecord {
    public static final String JNLP_SERVLET_PREFIX = "jnlp/client.jnlp?confId=";
    public static final String PORT_FIELD = "port";
    public static final String STATUS_FIELD = "status";
    public static final String JNLP_FIELD = "jnlp";

    public ConfigurationRecord() {
    }

    public ConfigurationRecord(JavaScriptObject jsObj) {
        super(jsObj);
    }

    public ConfigurationRecord(int id, String name, String description, Integer port, String status) {
        super(id, name, description);

        setPort(port);
        setStatus(status);
        setJnlp(GWT.getHostPageBaseURL() + JNLP_SERVLET_PREFIX + id);
    }

    private void setJnlp(String jnlp) {
        setAttribute(JNLP_FIELD, jnlp);
    }

    public String getJnlp() {
        return getAttributeAsString(JNLP_FIELD);
    }

    public void setPort(Integer port) {
        setAttribute(PORT_FIELD, port);
    }

    public Integer getPort() {
        return getAttributeAsInt(PORT_FIELD);
    }

    public void setStatus(String status) {
        setAttribute(STATUS_FIELD, status);
    }

    public String getStatus() {
        return getAttributeAsString(STATUS_FIELD);
    }

    public static ConfigurationRecord fromDTO(ConfigurationDTO dto) {
        return new ConfigurationRecord(dto.id, dto.name, dto.description, dto.port, dto.status);
    }

    public static ConfigurationRecord[] fromDTOs(ConfigurationDTO[] dtos) {
        ConfigurationRecord records[] = new ConfigurationRecord[dtos.length];
        for (int i = 0; i < dtos.length; i++) {
            records[i] = ConfigurationRecord.fromDTO(dtos[i]);
        }
        return records;
    }

    public ConfigurationDTO toDTO() {
        return new ConfigurationDTO(getId(), getName(), getDescription(), getPort(), getStatus());
    }
}
