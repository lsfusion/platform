package paas.api.gwt.shared.dto;

public class ModuleDTO extends BasicDTO {
    public String name;
    public String description;

    public ModuleDTO() {}

    public ModuleDTO(int id, String name, String description) {
        super(id);
        this.name = name;
        this.description = description;
    }
}
