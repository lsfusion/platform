package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.io.Serializable;
import java.util.Objects;

public class GPropertyFilterDTO implements Serializable {
    public int propertyID;
    public GFilterValueDTO filterValue;

    public GGroupObjectValue columnKey;

    public boolean negation;
    public byte compareByte;
    public boolean junction;

    // a filter the client sent equals the one the server reports for it (GFormController.currentFilters)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GPropertyFilterDTO)) return false;
        GPropertyFilterDTO that = (GPropertyFilterDTO) o;
        return propertyID == that.propertyID && filterValue.equals(that.filterValue) && Objects.equals(columnKey, that.columnKey) &&
                negation == that.negation && compareByte == that.compareByte && junction == that.junction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyID, filterValue, columnKey, negation, compareByte, junction);
    }
}
