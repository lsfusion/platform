package lsfusion.gwt.client.form.filter.user;

// WHERE THE CONDITION of a property's FILTER is - FILTER(<property>) React draws (GReactFormData.FilterPart), which reads
// and states it here -, one for all the FILTERs of a table, set up with its user filters
// (GReactFormData.createFiltersControllers): the platform's filter panel, where it shows them - its row of the FILTER,
// which it states as it states its rows, at once or held until its apply in its manual apply mode
// (GPlatformFiltersController) -, else the form's list, sent at once (GReactFormData.ListFilterConditions); none in
// PANEL view, which has no user filters (NONE)
public interface GFilterConditions {
    // the FILTER's condition, null for none
    GPropertyFilter getFilterCondition(GFilter filter);
    // ... a condition stated in its place
    void changeFilter(GFilter filter, GPropertyFilter condition);
    // ... and none
    void clearFilter(GFilter filter);

    GFilterConditions NONE = new GFilterConditions() {
        public GPropertyFilter getFilterCondition(GFilter filter) {
            return null;
        }
        public void changeFilter(GFilter filter, GPropertyFilter condition) {
        }
        public void clearFilter(GFilter filter) {
        }
    };
}
