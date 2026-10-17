package lsfusion.gwt.client.form.filter.user.view;

// the platform's EDITORS of a filter input controller's conditions - the panel's, where it draws the conditions
// (GPlatformFiltersController); none where a view draws them -, as the filter controls and the toolbar's button have
// them, of whichever kind each is (GFilterControlsToggle.bind): their selectors and delete buttons, shown and hidden
// with the controls, and the conditions typed into them, which wait to be applied in the client's manual apply mode
// (MainFrame.userFiltersManualApplyMode, while the controls are shown) - the apply button is theirs, shown by the mode
// and lit while something typed waits (GFilterControlsController.setApplyEnabled)
public interface GFilterEditors {
    void setConditionsControlsVisible(boolean visible);
    boolean isManualApplyMode();
    void applyFilters();
}
