package lsfusion.gwt.client.form.filter.user;

import java.io.Serializable;
import java.util.Objects;

public class GFilterValueDTO implements Serializable {
    public Serializable content;

    public GFilterValueDTO() {
    }

    public GFilterValueDTO(Serializable content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GFilterValueDTO)) return false;
        GFilterValueDTO that = (GFilterValueDTO) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(content);
    }
}
