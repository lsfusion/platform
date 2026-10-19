package lsfusion.gwt.client.form.filter.user;

// the form's controller of a property's FILTER - FILTER(<property>), the user filter declared on a property -, of the
// kind of whoever draws it, chosen once (GReactFormData.createFilterController), as a filter group's is: a view's part
// of the group of its property (GReactFormData.FilterPart), or, where the platform draws it, nothing (NONE) - its
// filter panel makes it, a condition of the panel, and is told of the group's user filters as what shows the group's
// FILTERS box (GUserFiltersController). The condition it shows is of the group's user filters - the first on its
// property among them, or, over the platform's panel, the panel's row of it -, so it is told whenever they change,
// whoever changes them: the server reporting them, or the form sending them
public interface GFilterController {
    // the group's user filters changed: the form has them now (GFormController.getFilters)
    void updateCondition();

    GFilterController NONE = new GFilterController() {
        public void updateCondition() {
        }
    };
}
