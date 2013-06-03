package lsfusion.gwt.form.shared.view.filter;

import lsfusion.gwt.form.shared.view.GGroupObject;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.dto.GPropertyFilterDTO;

public class GPropertyFilter {
    public GGroupObject groupObject;
    public GPropertyDraw property;
    public GFilterValue value;

    public boolean negation;
    public GCompare compare;
    public boolean junction = true; //true - conjunction, false - disjunction

    public GPropertyFilterDTO getFilterDTO() {
        GPropertyFilterDTO filterDTO = new GPropertyFilterDTO();

        filterDTO.propertyID = property.ID;
        filterDTO.filterValue = value.getDTO();
        filterDTO.negation = negation;
        filterDTO.compareByte = compare.serialize();
        filterDTO.junction = junction;

        return filterDTO;
    }
}
