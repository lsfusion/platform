package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.form.property.cell.classes.GZDateTimeDTO;
import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.*;

// A filter a client sends and the one the server reports back for it are equal by their values, not as the same objects
public class GPropertyFilterDTOTest {

    private static GPropertyFilterDTO filter(int propertyID, Serializable value, boolean negation, boolean junction) {
        GPropertyFilterDTO filter = new GPropertyFilterDTO();
        filter.propertyID = propertyID;
        filter.filterValue = new GFilterValueDTO(value);
        filter.negation = negation;
        filter.compareByte = 1;
        filter.junction = junction;
        return filter;
    }

    @Test
    public void theSameConditionIsEqual() {
        GPropertyFilterDTO sent = filter(7, "milk", false, true);
        GPropertyFilterDTO reported = filter(7, "milk", false, true);
        assertEquals(sent, reported);
        assertEquals(sent.hashCode(), reported.hashCode());
    }

    @Test
    public void anotherValueNegationOrJunctionIsAnotherCondition() {
        GPropertyFilterDTO sent = filter(7, "milk", false, true);
        assertNotEquals(sent, filter(7, "bread", false, true));
        assertNotEquals(sent, filter(7, "milk", true, true));
        assertNotEquals(sent, filter(7, "milk", false, false));
        assertNotEquals(sent, filter(8, "milk", false, true));
    }

    @Test
    public void aDateTimeValueIsEqualByItsInstant() {
        assertEquals(filter(7, new GZDateTimeDTO(1000L), false, true), filter(7, new GZDateTimeDTO(1000L), false, true));
        assertNotEquals(filter(7, new GZDateTimeDTO(1000L), false, true), filter(7, new GZDateTimeDTO(2000L), false, true));
    }
}
