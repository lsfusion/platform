package paas.api.gwt.shared.dto;

public class ConfigurationDTO extends BasicDTO {
    public Integer port;
    public String status;

    public ConfigurationDTO() {}

    public ConfigurationDTO(int id, String name, String description, Integer port, String status) {
        super(id, name, description);
        this.port = port;
        this.status = status;
    }
}
