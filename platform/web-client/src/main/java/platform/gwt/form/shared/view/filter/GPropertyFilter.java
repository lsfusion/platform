package platform.gwt.form.shared.view.filter;

import platform.gwt.form.shared.view.GGroupObject;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.dto.GPropertyFilterDTO;

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
        filterDTO.filterValue = value;
        filterDTO.negation = negation;
        filterDTO.compareByte = compare.serialize();
        filterDTO.junction = junction;

        return filterDTO;
    }
}
