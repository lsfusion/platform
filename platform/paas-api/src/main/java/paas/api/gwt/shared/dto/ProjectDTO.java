package paas.api.gwt.shared.dto;

public class ProjectDTO extends BasicDTO {
    public String name;
    public String description;

    public ProjectDTO() {}

    public ProjectDTO(int id, String name, String description) {
        super(id);
        this.name = name;
        this.description = description;
    }
}
