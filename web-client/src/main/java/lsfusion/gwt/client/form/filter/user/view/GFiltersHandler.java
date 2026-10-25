package lsfusion.gwt.client.form.filter.user.view;

// the commands of a filter input controller its controls give, of whichever kind either is (GFilterControlsController):
// the add - where the design has the FILTERS box, as the button is there only then -, the apply of what waits for it
// in the manual apply mode (GUserFiltersController.apply), and the reset
public interface GFiltersHandler {
    boolean hasFiltersContainer();

    void addCondition();

    void applyConditions();

    void resetConditions();
}
