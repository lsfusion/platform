package paas.api.gwt.shared.dto;

public class ConfigurationDTO extends BasicDTO {
    public Integer port;
    public String exportName;
    public String status;

    public ConfigurationDTO() {}

    public ConfigurationDTO(int id, String name, String description, Integer port, String exportName, String status) {
        super(id, name, description);
        this.port = port;
        this.exportName = exportName;
        this.status = status;
    }
}
