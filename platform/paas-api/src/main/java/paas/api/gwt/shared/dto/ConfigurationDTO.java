package paas.api.gwt.shared.dto;

public class ConfigurationDTO extends BasicDTO {
    public String name;
    public String description;
    public Integer port;
    public String status;

    public ConfigurationDTO() {}

    public ConfigurationDTO(int id, String name, String description, Integer port, String status) {
        super(id);
        this.name = name;
        this.description = description;
        this.port = port;
        this.status = status;
    }
}
