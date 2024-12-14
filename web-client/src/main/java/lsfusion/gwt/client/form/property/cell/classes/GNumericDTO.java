package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;
import java.util.Objects;

public class GNumericDTO implements Serializable {
    // in theory in formatString and formatISOString we could support something like BigDecimal (not limited double), but in all other places - not, so for now we'll use double
    public double value;

    @SuppressWarnings("UnusedDeclaration")
    public GNumericDTO() {}

    public GNumericDTO(double value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GNumericDTO)) return false;
        return value == (((GNumericDTO) o).value);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }
}
