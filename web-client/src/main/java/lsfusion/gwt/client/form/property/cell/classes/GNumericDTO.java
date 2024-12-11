package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;
import java.util.Objects;

public class GNumericDTO implements Serializable {
    public String value;

    @SuppressWarnings("UnusedDeclaration")
    public GNumericDTO() {}

    public GNumericDTO(String value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GNumericDTO)) return false;
        return value.equals(((GNumericDTO) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
