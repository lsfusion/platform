package paas.api.gwt.shared.dto;

import java.io.Serializable;

public class BasicDTO implements Serializable {
    public int id;
    public String name;
    public String description;

    public BasicDTO() {}

    public BasicDTO(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
