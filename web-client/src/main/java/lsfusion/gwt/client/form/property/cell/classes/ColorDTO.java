package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;

public class ColorDTO implements Serializable {
    public String value;

    @SuppressWarnings("UnusedDeclaration")
    public ColorDTO() {}

    public ColorDTO(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "#" + value;
    }

    // a value DTO: two colors with the same value ARE the same color. Without this, a PValue holding a ColorDTO compares by
    // identity (SerializableValue.equals delegates to the wrapped value), so a re-delivered identical color reads as a change
    // and needlessly dirties its cell / row.
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ColorDTO))
            return false;
        String other = ((ColorDTO) o).value;
        return value == null ? other == null : value.equals(other);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
