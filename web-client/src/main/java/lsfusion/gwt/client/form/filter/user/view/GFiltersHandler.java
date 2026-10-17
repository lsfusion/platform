package lsfusion.gwt.client.form.filter.user.view;

// the commands of a filter input controller its controls give, of whichever kind either is (GFilterControlsController):
// the add - where the design has the FILTERS box, as the button is there only then - and the reset. The apply is the
// platform's editors' (GFilterEditors): nothing waits for it where a view draws the conditions
public interface GFiltersHandler {
    boolean hasFiltersContainer();

    void addCondition();

    void resetConditions();
}
