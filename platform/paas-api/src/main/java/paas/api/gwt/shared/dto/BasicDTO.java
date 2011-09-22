package paas.api.gwt.shared.dto;

import java.io.Serializable;

public class BasicDTO implements Serializable {
    public int id;

    public BasicDTO() {}

    public BasicDTO(int id) {
        this.id = id;
    }
}
